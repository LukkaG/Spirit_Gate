# Marco G — Sprites temporárias e camadas

TASK-156 gerou 180 PNGs Java2D para três cabelos e três roupas, derivados das 20 poses e pivôs reais, com transparência binária e manifesto versionado. Os hashes dos corpos originais foram preservados.

TASK-157 integrou catálogo limitado, validação antes de decodificar, composição por planos, preview de quatro direções/Idle/Walk e perfil sintético independente do save V2. A pintura usa somente recursos residentes e mantém fallback por camada. Capturas de todas as combinações estão no checkpoint, sem aprovação artística implícita.

Regressão: 460 executados, 452 aprovados, oito abortados headless, zero falhas e zero ignorados. Studio gráfico: 57 aprovados. A correção da fronteira dos checkpoints 156/157 foi registrada e preservou cópias dos arquivos anteriores.
