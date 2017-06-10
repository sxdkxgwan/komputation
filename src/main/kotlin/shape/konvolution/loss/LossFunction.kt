package shape.konvolution.loss

import shape.konvolution.matrix.RealMatrix

interface LossFunction {

    fun forward(predictions: RealMatrix, targets : RealMatrix): Double

    fun backward(predictions: RealMatrix, targets : RealMatrix): RealMatrix

}