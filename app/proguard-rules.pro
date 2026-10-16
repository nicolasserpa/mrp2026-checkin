# R8 full mode (AGP 8). ZXing no caminho QR usa instanciação direta
# (MultiFormatReader -> new QRCodeReader), sem reflexão: nenhum -keep no
# zxing — os decoders não usados (aztec, datamatrix, pdf417...) são podados.
# Se o scan quebrar no aparelho após o release, iterar aqui antes de culpar
# o decode.
-keepattributes SourceFile,LineNumberTable
