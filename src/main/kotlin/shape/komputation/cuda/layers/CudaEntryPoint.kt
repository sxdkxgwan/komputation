package shape.komputation.cuda.layers

import jcuda.Pointer
import shape.komputation.matrix.Matrix

interface CudaEntryPoint {

    fun forward(id : Int, input: Matrix) : Pointer

    fun backward(chain : Pointer) : Pointer

}