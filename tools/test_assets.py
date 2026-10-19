"""Fixtures sinteticas de validacao de assets, nunca arte final."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from validar_assets import inspect, run, CONTRACT, DIRECTIONS
from separar_sprites import write_png


class AssetTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def tile(self, size=32):
        path = self.root/'tile.png'
        write_png(path,size,size,bytearray([20,100,20,255])*size*size)
        return {'path':'tile.png','kind':'tile','grid':32,'frames':[], 'pivot':[0,0],
                'offset':[0,0],'layer':'ground','origin':'fixture sintetica',
                'review':{'approvedSha256':hashlib.sha256(path.read_bytes()).hexdigest()}}

    def character(self):
        frames=[]
        for s,n in CONTRACT.items():
            for d in DIRECTIONS:
                for i in range(n):
                    frames.append(dict(state=s,direction=d,index=i,x=len(frames)*64,y=0,width=64,height=64))
        for i in range(3):
            frames.append(dict(state='KO',direction='FRONT',index=i,x=len(frames)*64,y=0,width=64,height=64))
        w=59*64
        pixels=bytearray(w*64*4)
        for f in frames:
            at=(32*w+f['x']+32)*4
            pixels[at:at+4]=bytes([255,0,0,255])
        path=self.root/'hero.png'
        write_png(path,w,64,pixels)
        return dict(path='hero.png',kind='character',grid=64,frames=frames,pivot=[32,48],offset=[0,0],
                    layer='actors',origin='fixture sintetica',review={'approvedSha256':hashlib.sha256(path.read_bytes()).hexdigest()})

    def test_valid_and_deterministic(self):
        entry=self.tile()
        self.assertEqual(inspect(self.root,entry)[0]['status'],'VALIDADO')
        manifest,report,atlas=(self.root/n for n in ('manifest.json','report.json','atlas.png'))
        manifest.write_text(json.dumps({'assets':[entry]}))
        # Atlas fora da raiz inventariada para nao inventariar a propria saida.
        atlas=self.root.parent/(self.root.name+'-atlas.png')
        self.addCleanup(lambda:atlas.unlink(missing_ok=True))
        run(self.root,manifest,report,atlas)
        first=report.read_bytes()
        run(self.root,manifest,report,atlas)
        self.assertEqual(first,report.read_bytes())

    def test_invalid_dimensions(self):
        self.assertEqual(inspect(self.root,self.tile(31))[0]['status'],'REPROVADO')

    def test_character_contract_and_missing_frame(self):
        entry=self.character()
        self.assertEqual(inspect(self.root,entry)[0]['status'],'VALIDADO')
        entry['frames'].pop()
        self.assertIn('contrato de 59 frames incompleto',inspect(self.root,entry)[0]['errors'])

    def test_background_and_overlap(self):
        entry=self.character()
        write_png(self.root/'hero.png',59*64,64,bytearray([80,80,80,255])*59*64*64)
        result=inspect(self.root,entry)[0]
        self.assertEqual(result['status'],'REPROVADO')
        self.assertIn('fundo opaco em personagem transparente',result['errors'])
        entry['frames'][1]['x']=0
        self.assertIn('frames sobrepostos',inspect(self.root,entry)[0]['errors'])

    def test_hash_bound_approval(self):
        entry=self.tile()
        entry['review']={}
        self.assertEqual(inspect(self.root,entry)[0]['status'],'PENDENTE')

    def test_export_contract_and_invalid_pivot(self):
        entry=self.character()
        manifest,report=(self.root/n for n in ('manifest.json','report.json'))
        manifest.write_text(json.dumps({'assets':[entry]}))
        atlas=self.root.parent/(self.root.name+'-atlas.png')
        self.addCleanup(lambda:atlas.unlink(missing_ok=True))
        run(self.root,manifest,report,atlas)
        exported=(self.root/'hero-validated.properties').read_text()
        self.assertIn('status=VALIDADO',exported)
        self.assertEqual(59,sum(line.startswith(tuple(CONTRACT)+('KO',)) for line in exported.splitlines()))
        self.assertIn('PUNCH.FRONT.2=',exported)
        entry['pivot']=[999,0]
        self.assertIn('pivot fora do frame',inspect(self.root,entry)[0]['errors'])

    def test_corrupt_png(self):
        entry=self.tile()
        png=self.root/'tile.png'
        data=bytearray(png.read_bytes());data[20]^=1;png.write_bytes(data)
        self.assertEqual(inspect(self.root,entry)[0]['status'],'REPROVADO')


if __name__=='__main__':
    unittest.main()
