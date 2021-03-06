package shape.komputation.cpu.functions

fun repeatColumn(column: FloatArray, repetition : FloatArray, numberRepetitions: Int) {

    val numberRows = column.size

    for(indexColumn in 0..numberRepetitions - 1) {

        val start = indexColumn * numberRows

        for (indexRow in 0..numberRows - 1) {

            repetition[start+indexRow] = column[indexRow]

        }

    }

}