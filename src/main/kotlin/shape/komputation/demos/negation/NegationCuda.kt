package shape.komputation.demos.negation

import shape.komputation.cpu.printLoss
import shape.komputation.cuda.CudaNetwork
import shape.komputation.initialization.heInitialization
import shape.komputation.layers.entry.inputLayer
import shape.komputation.layers.forward.activation.sigmoidLayer
import shape.komputation.layers.forward.projection.projectionLayer
import shape.komputation.loss.squaredLoss
import shape.komputation.optimization.stochasticGradientDescent
import java.util.*

fun main(args: Array<String>) {

    val inputDimension = 1
    val outputDimension = 1

    val random = Random(1)
    val initialize = heInitialization(random)

    val optimization = stochasticGradientDescent(0.01)

    val network = CudaNetwork(
        inputLayer(inputDimension),
        projectionLayer(inputDimension, outputDimension, initialize, initialize, optimization),
        sigmoidLayer(outputDimension)
    )

    val time = network.train(NegationData.inputs, NegationData.targets, squaredLoss(outputDimension), 10_000, 1, printLoss)

    println(time)

}