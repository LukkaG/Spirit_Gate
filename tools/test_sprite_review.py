"""Revisao usa apenas fixtures sinteticas temporarias."""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from revisar_sprites import frames, template, review, aseprite
from separar_sprites import write_png
from validar_assets import read_png


class SpriteReviewTest(unittest.TestCase):
    def test_directional_preview_and_version_comparison(self):
        from revisar_sprites import compare
        result=self.check()
        preview=(self.root/'out1/preview.html').read_text(encoding='utf-8')
        for name in ('IDLE.FRONT','WALK.LEFT','RUN.RIGHT','PUNCH.BACK','KO.FRONT'):
            self.assertIn(name,preview)
        self.assertIn('@keyframes',preview)
        self.assertIn('../hero.png',preview)
        self.assertNotIn(self.root.as_uri(),preview)
        comparison=compare(self.root/'hero.png',self.root/'hero.png',self.root/'same.json')
        self.assertTrue(comparison['sameBytes']);self.assertFalse(comparison['artApproved'])
        self.pixels[(32*512+32)*4]=10
        write_png(self.root/'other.png',512,512,self.pixels)
        self.assertFalse(compare(self.root/'hero.png',self.root/'other.png',self.root/'different.json')['sameBytes'])
        with self.assertRaises(FileExistsError):compare(self.root/'hero.png',self.root/'other.png',self.root/'same.json')

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.pixels = bytearray(512*512*4)
        for f in frames():
            i = ((f['y']+32)*512+f['x']+32)*4
            self.pixels[i:i+4] = bytes([255,0,0,255])
        write_png(self.root/'hero.png',512,512,self.pixels)
        self.entry = dict(path='hero.png',kind='character',grid=64,frames=frames(),pivot=[32,48],offset=[0,0],layer='actors',origin='FIXTURE SINTETICA',review={})
        self.count = 0

    def check(self, entry=None):
        self.count += 1
        m = self.root/'manifest.json'; m.write_text(json.dumps({'assets':[entry or self.entry]}))
        return review(self.root,m,self.root/f'out{self.count}')[0]

    def test_template_59_empty_reproducible(self):
        template(self.root/'a'); template(self.root/'b')
        self.assertEqual((self.root/'a/template-vazio.png').read_bytes(),(self.root/'b/template-vazio.png').read_bytes())
        w,h,p = read_png(self.root/'a/template-vazio.png')
        self.assertEqual((w,h),(512,512)); self.assertFalse(any(p))
        self.assertEqual(len(frames()),59)
        self.assertEqual(len({(f['state'],f['direction'],f['index']) for f in frames()}),59)

    def test_direction_order_missing_pivot_overlap(self):
        for mutation in ('direction','order','missing','pivot','overlap'):
            e = copy.deepcopy(self.entry)
            if mutation == 'direction': e['frames'][0]['direction']='RIGHT'
            if mutation == 'order': e['frames'][0],e['frames'][1]=e['frames'][1],e['frames'][0]
            if mutation == 'missing': e['frames'].pop()
            if mutation == 'pivot': e['frames'][0]['pivot']=[0,0]
            if mutation == 'overlap': e['frames'][1]['x']=0
            with self.subTest(mutation=mutation): self.assertEqual(self.check(e)['status'],'REPROVADO')

    def test_alpha_border_and_hash_after_review(self):
        self.entry['review']['approvedSha256']=hashlib.sha256((self.root/'hero.png').read_bytes()).hexdigest()
        self.assertEqual(self.check()['status'],'PENDENTE_REVISAO_HUMANA')
        self.pixels[3]=128
        write_png(self.root/'hero.png',512,512,self.pixels)
        result=self.check()
        self.assertIn('alfa parcial: revisar antialiasing/fundo irregular',result['errors'])
        self.assertNotEqual(result['sha256'],self.entry['review']['approvedSha256'])
        self.assertFalse(result['reviewToolApprovesArt'])

    def test_aseprite_exact_export_and_reject_trim_order(self):
        exported=[]
        for f in frames():
            exported.append(dict(filename=f"{f['state']}.{f['direction']}.{f['index']}",rotated=False,trimmed=False,duration=100,sourceSize={'w':64,'h':64},spriteSourceSize={'x':0,'y':0,'w':64,'h':64},frame={'x':f['x'],'y':f['y'],'w':64,'h':64}))
        data={'frames':exported,'meta':{'size':{'w':512,'h':512}}}
        p=self.root/'ase.json'; p.write_text(json.dumps(data))
        aseprite(self.root/'hero.png',p,self.root/'import.json')
        for field,value in [('trimmed',True),('filename','IDLE.RIGHT.0')]:
            bad=copy.deepcopy(data); bad['frames'][0][field]=value; p.write_text(json.dumps(bad))
            with self.assertRaises(ValueError): aseprite(self.root/'hero.png',p,self.root/'bad.json')
