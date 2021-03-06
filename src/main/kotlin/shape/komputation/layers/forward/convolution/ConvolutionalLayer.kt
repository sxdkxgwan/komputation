package shape.komputation.layers.forward.convolution

import shape.komputation.cpu.layers.forward.convolution.CpuConvolutionalLayer
import shape.komputation.initialization.InitializationStrategy
import shape.komputation.layers.CpuForwardLayerInstruction
import shape.komputation.layers.concatenateNames
import shape.komputation.layers.forward.projection.projectionLayer
import shape.komputation.optimization.OptimizationInstruction

class ConvolutionalLayer(
    private val name : String?,
    private val numberFilters: Int,
    private val filterWidth: Int,
    private val filterHeight : Int,
    private val weightInitialization: InitializationStrategy,
    private val biasInitialization: InitializationStrategy,
    private val optimization: OptimizationInstruction? = null) : CpuForwardLayerInstruction {

    override fun buildForCpu(): CpuConvolutionalLayer {

        val expansionLayerName = concatenateNames(name, "expansion")
        val expansionLayer = expansionLayer(expansionLayerName, this.filterWidth, this.filterHeight).buildForCpu()

        val projectionLayerName = concatenateNames(name, "projection")
        val projectionLayer = projectionLayer(projectionLayerName, this.filterWidth * this.filterHeight, this.numberFilters, this.weightInitialization, this.biasInitialization, this.optimization).buildForCpu()

        return CpuConvolutionalLayer(name, expansionLayer, projectionLayer)

    }


}

fun convolutionalLayer(
    numberFilters: Int,
    filterWidth: Int,
    filterHeight : Int,
    weightInitialization: InitializationStrategy,
    biasInitialization: InitializationStrategy,
    optimization: OptimizationInstruction? = null): ConvolutionalLayer {

    return convolutionalLayer(null, numberFilters, filterWidth, filterHeight, weightInitialization, biasInitialization, optimization)

}

fun convolutionalLayer(
    name : String?,
    numberFilters: Int,
    filterWidth: Int,
    filterHeight : Int,
    weightInitialization: InitializationStrategy,
    biasInitialization: InitializationStrategy,
    optimization: OptimizationInstruction? = null) =

    ConvolutionalLayer(name, numberFilters, filterWidth, filterHeight, weightInitialization, biasInitialization, optimization)