"""Controles negativos de nomes ZIP, sem executar conteudo do pacote."""
import io
import stat
import unittest
import zipfile
from auditar_rc import archive_files, AuditFailure


class ZipSecurityTest(unittest.TestCase):
    def archive(self, names):
        data = io.BytesIO()
        with zipfile.ZipFile(data, 'w') as out:
            for name in names:
                out.writestr(name, b'teste')
        data.seek(0)
        return zipfile.ZipFile(data)

    def test_canonical(self):
        with self.archive(['SpiritGate/app/a.jar']) as archive:
            self.assertEqual(list(archive_files(archive)), ['SpiritGate/app/a.jar'])

    def test_unsafe_names(self):
        for name in ['SpiritGate\\app/a', '/a', '../a', 'a/../b', 'a:b',
                     'a//b', 'a/NUL.txt', 'a/COM1', 'a/COM¹', 'a/CONOUT$', 'a/x.', 'a/x ', 'a/\x00hidden']:
            with self.subTest(name=name), self.archive(['placeholder']) as archive:
                # O escritor Python no Windows normaliza barras e trunca NUL;
                # simula o nome cru malformado que o leitor precisa recusar.
                archive.infolist()[0].orig_filename = name
                with self.assertRaises(AuditFailure):
                    archive_files(archive)

    def test_original_before_null_truncation(self):
        with self.archive(['safe']) as archive:
            archive.infolist()[0].orig_filename = 'safe\x00hidden'
            with self.assertRaises(AuditFailure):
                archive_files(archive)

    def test_case_collision_and_file_parent(self):
        for names in [['a/X', 'a/x'], ['a', 'a/b']]:
            with self.archive(names) as archive, self.assertRaises(AuditFailure):
                archive_files(archive)

    def test_symlink(self):
        with self.archive(['a']) as archive:
            archive.infolist()[0].external_attr = (stat.S_IFLNK | 0o777) << 16
            with self.assertRaises(AuditFailure):
                archive_files(archive)


if __name__ == '__main__':
    unittest.main()
