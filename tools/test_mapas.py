import tempfile
from pathlib import Path
import unittest
from mapas import create,validate,safe_path

class MapToolTest(unittest.TestCase):
    def test_generated_maps_load_in_real_java_and_are_deterministic(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            a=create(root,'a.properties');b=create(root,'b.properties')
            self.assertEqual(a.read_bytes(),b.read_bytes())
            self.assertIn('VALIDO 1920x1280',validate(a))
            self.assertIn('layer.6=1',validate(a))
            self.assertIn('VALIDO 960x640',validate(create(root,'v1.properties',960,640)))
            with self.assertRaises(FileExistsError):create(root,'a.properties')

    def test_escape_excessive_and_invalid_fields_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            with self.assertRaises(ValueError):safe_path(root,'../escape')
            path=create(root,'bad.properties')
            original=path.read_text()
            for old,new in [('spawnX=135','spawnX=NaN'),('visual.0.x=32','visual.0.x=-1'),
                            ('visual.1.id=synthetic-1','visual.1.id=synthetic-0'),
                            ('physical.0.width=1920','physical.0.width=1900')]:
                path.write_text(original.replace(old,new))
                with self.subTest(field=old),self.assertRaises(ValueError):validate(path)
            path.write_bytes(b'x'*1_000_001)
            with self.assertRaises(ValueError):validate(path)
