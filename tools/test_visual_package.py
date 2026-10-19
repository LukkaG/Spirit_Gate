"""Distribuicao visual exercitada apenas em diretorios temporarios sinteticos."""
import hashlib
import copy
import json
import unittest
import zipfile
import test_sprite_review
from pacote_visual import stage


class VisualPackageTest(unittest.TestCase):
    def setUp(self):
        test_sprite_review.SpriteReviewTest.setUp(self)

    def config(self):
        self.entry['review']={'origin':'FIXTURE SINTETICA - teste, nao aprovacao humana',
            'approvedSha256':hashlib.sha256((self.root/'hero.png').read_bytes()).hexdigest()}
        return dict(packageVersion=1,assetId='synthetic.hero',assets=[self.entry])

    def test_stage_approved_fixture_exact_whitelist(self):
        p=self.root/'package.json'; p.write_text(json.dumps(self.config()))
        (self.root/'pending.png').write_bytes(b'NAO INCLUIR')
        stage(self.root,p,self.root/'visual')
        self.assertEqual({p.name for p in (self.root/'visual').iterdir()}, {'review.json','character.properties','character.png'})
        self.assertEqual((self.root/'hero.png').read_bytes(),(self.root/'visual/character.png').read_bytes())
        self.assertIn('packageVersion=1',(self.root/'visual/character.properties').read_text())
        with zipfile.ZipFile(self.root/'synthetic-distribution.zip','w') as z:
            for p in (self.root/'visual').iterdir(): z.write(p,'SpiritGate/visual/'+p.name)
        with zipfile.ZipFile(self.root/'synthetic-distribution.zip') as z:
            self.assertEqual(set(z.namelist()),{'SpiritGate/visual/'+n for n in ('review.json','character.properties','character.png')})

    def test_reject_pending_missing_corrupt_hash_no_output(self):
        original=copy.deepcopy(self.entry)
        for case in ('pending','missing','hash','pivot','duration','offset','corrupt'):
            self.entry=copy.deepcopy(original)
            config=self.config()
            if case=='pending': self.entry['review']={}
            if case=='missing': self.entry['frames'].pop()
            if case=='hash': self.entry['review']['approvedSha256']='0'*64
            if case=='pivot': self.entry['frames'][0]['pivot']=[0,0]
            if case=='duration': self.entry['frames'][0]['durationMs']=200
            if case=='offset': self.entry['offset']=[2147483647,0]
            if case=='corrupt': (self.root/'hero.png').write_bytes(b'corrupt')
            p=self.root/'package.json'; p.write_text(json.dumps(config))
            with self.subTest(case=case),self.assertRaises(ValueError): stage(self.root,p,self.root/case)
            self.assertFalse((self.root/case).exists())
