"""Regressões de classificação e leitura de resultados do orquestrador."""
import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch
import qualidade

class QualityTest(unittest.TestCase):
    def test_exit_categories(self):
        self.assertEqual(0, qualidade.classify(0, 'BUILD SUCCESS'))
        self.assertEqual(3, qualidade.classify(1, 'There are test failures'))
        self.assertEqual(3, qualidade.classify(1, 'FAILED (failures=1)'))
        self.assertEqual(4, qualidade.classify(1, 'COMPILATION ERROR'))
        self.assertEqual(4, qualidade.classify(1, 'Could not resolve dependencies'))

    def test_real_totals_and_missing_report(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            with self.assertRaises(ValueError):
                qualidade.junit_totals(folder)
            (folder / 'TEST-a.xml').write_text('<testsuite tests="3" failures="1" errors="0" skipped="1"/>')
            self.assertEqual({'tests': 3, 'failures': 1, 'errors': 0, 'skipped': 1},
                             qualidade.junit_totals(folder))

    def test_missing_environment_writes_failure(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(qualidade, 'ROOT', Path(directory)):
            (Path(directory) / 'docs').mkdir()
            with patch.object(qualidade.shutil, 'which', return_value=None):
                self.assertEqual(2, qualidade.main(['--task', 'negative']))
            self.assertIn('Ferramenta ausente', (Path(directory) / 'docs/TASK-negative-qualidade.json').read_text(encoding='utf-8'))

    def test_identifier_cannot_escape(self):
        with self.assertRaises(SystemExit):
            qualidade.main(['--task', '../../escape'])
