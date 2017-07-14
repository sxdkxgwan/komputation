package shape.komputation.cuda.layers.forward.projection

import jcuda.Pointer
import jcuda.jcublas.JCublas2.cublasCreate
import jcuda.jcublas.JCublas2.cublasDestroy
import jcuda.jcublas.cublasHandle
import jcuda.runtime.JCuda.cudaFree
import shape.komputation.cuda.allocateDeviceMemory
import shape.komputation.cuda.copyFromHostToDevice
import shape.komputation.cuda.functions.cublasBackwardProjectionWrtBias
import shape.komputation.cuda.functions.cublasBackwardProjectionWrtInput
import shape.komputation.cuda.functions.cublasBackwardProjectionWrtWeights
import shape.komputation.cuda.functions.cublasProject
import shape.komputation.cuda.layers.BaseCudaForwardLayer
import shape.komputation.cuda.optimization.CublasUpdateRule
import shape.komputation.cuda.setVectorToZero
import shape.komputation.layers.Resourceful
import shape.komputation.optimization.Optimizable

class CublasProjectionLayer internal constructor(
    name: String?,
    private val cublasHandle: cublasHandle,
    private val initialWeights: DoubleArray,
    private val numberWeightRows: Int,
    private val numberWeightColumns: Int,
    private val weightUpdateRule: CublasUpdateRule? = null,

    private val initialBias: DoubleArray? = null,
    private val biasUpdateRule: CublasUpdateRule? = null) : BaseCudaForwardLayer(name), Optimizable, Resourceful {

    private val numberWeightEntries = this.numberWeightRows * this.numberWeightColumns

    private val numberBiasEntries = if(this.initialBias != null) this.initialBias.size else 0

    private val inputDimension = this.numberWeightColumns
    private val resultDimension = this.numberWeightRows
    private val chainDimension = resultDimension

    /*
                       i_1
                       i_2
                       i_3
        w_11 w_12 w_13
        w_21 w_22 w_23

        input dimension = number of weight columns
        result dimension = number of weight rows
    */

    var deviceResult = Pointer()

    var deviceWeights = Pointer()
    var deviceWeightGradientAccumulator = Pointer()

    var deviceBias = Pointer()
    var deviceBiasGradientAccumulator = Pointer()

    var deviceBackwardWrtInput = Pointer()

    override fun acquire() {

        cublasCreate(this.cublasHandle)

        allocateDeviceMemory(this.deviceResult, this.numberWeightRows)

        allocateDeviceMemory(this.deviceBackwardWrtInput, this.inputDimension)
        allocateDeviceMemory(this.deviceWeightGradientAccumulator, this.numberWeightEntries)
        allocateDeviceMemory(this.deviceBiasGradientAccumulator, this.numberBiasEntries)

        copyFromHostToDevice(this.initialWeights, this.numberWeightEntries, this.deviceWeights)

        if(this.initialBias != null) {

            copyFromHostToDevice(this.initialBias, this.numberBiasEntries, this.deviceBias)

        }

    }

    private var deviceInput = Pointer()

    override fun forward(input : Pointer): Pointer {

        this.deviceInput = input

        cublasProject(this.cublasHandle, this.deviceInput, this.deviceResult, this.deviceWeights, this.numberWeightRows, this.numberWeightColumns, this.deviceBias, this.numberBiasEntries)

        return this.deviceResult

    }

    /*
                          x_1
                          x_2
                          x_3
        w_11 w_12 w_13    w_11 * x_1 + w_12 * x_2 + w_13 * x_3
        w_21 w_22 w_23    w_21 * x_1 + w_22 * x_2 + w_23 * x_3

        Differentiation w.r.t input:

        d Wx / d x = w_11 + w_21
                     w_12 + w_22
                     w_13 + w_23

        gemv solution:
                                  chain_1
                                  chain_2
        transposed W >> w_11 w_21
                        w_12 w_22
                        w_13 w_23

        Differentiation w.r.t weights:

        d Wx / d W = x_1 x_2 x_3
                     x_1 x_2 x_3

        ger solution:
                x1 x2 x3 << transposed x
        chain_1
        chain_2

     */

    override fun backward(chain: Pointer): Pointer {

        cublasBackwardProjectionWrtInput(this.cublasHandle, this.deviceWeights, this.numberWeightRows, this.numberWeightColumns, chain, this.deviceBackwardWrtInput)
        cublasBackwardProjectionWrtWeights(this.cublasHandle, this.deviceInput, chain, this.deviceWeightGradientAccumulator, this.numberWeightRows, this.numberWeightColumns)

        if (this.initialBias != null) {

            cublasBackwardProjectionWrtBias(this.cublasHandle, chain, this.chainDimension, this.deviceBiasGradientAccumulator)

        }

        return this.deviceBackwardWrtInput

    }

    override fun optimize(scalingFactor: Double) {

        if (this.weightUpdateRule != null) {

            this.weightUpdateRule.update(this.deviceWeights, scalingFactor, this.deviceWeightGradientAccumulator)
            setVectorToZero(this.deviceWeightGradientAccumulator, this.numberWeightEntries)

        }

        if (this.biasUpdateRule != null) {

            this.biasUpdateRule.update(this.deviceBias, scalingFactor, this.deviceBiasGradientAccumulator)
            setVectorToZero(this.deviceBiasGradientAccumulator, this.numberBiasEntries)

        }

    }

    override fun release() {

        cudaFree(this.deviceResult)

        cudaFree(this.deviceWeights)
        cudaFree(this.deviceWeightGradientAccumulator)

        if(this.initialBias != null) {

            cudaFree(this.deviceBias)
            cudaFree(this.deviceBiasGradientAccumulator)

        }

        cudaFree(this.deviceBackwardWrtInput)

        cublasDestroy(this.cublasHandle)

    }

}