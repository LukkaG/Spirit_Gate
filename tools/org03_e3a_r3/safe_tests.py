"""Executor de testes R3-01 com rede/processos/remoção operacional proibidos."""
import sys
import unittest
from unittest.mock import patch
from pathlib import Path


def forbidden(*args,**kwargs):
    """Uma dependência real acidental é falha de teste, nunca homologação."""
    raise AssertionError('R3_01_FORBIDDEN_REAL_OPERATION')


def main():
    """Processo Python do teste é permitido; nenhum subprocesso interno ou socket."""
    root=Path(__file__).resolve().parents[2]
    scripts=root/'temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/scripts'
    location=scripts/'legacy' if sys.argv[1]=='legacy' else scripts
    pattern={'legacy':'test_harness.py','p1':'test_transport_fix.py','integration':'test_runner_integration.py'}[sys.argv[1]]
    sys.path.insert(0,str(location))
    # Guardas globais permanecem ativas durante importação e execução dos testes.
    from contextlib import ExitStack
    with ExitStack() as stack:
        for target in ('subprocess.Popen','subprocess.run','socket.socket','socket.create_connection','shutil.rmtree'):
            stack.enter_context(patch(target,side_effect=forbidden))
        suite=unittest.defaultTestLoader.discover(str(location),pattern=pattern)
        result=unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__=='__main__': raise SystemExit(main())
