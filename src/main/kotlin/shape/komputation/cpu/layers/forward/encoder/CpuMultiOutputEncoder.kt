package shape.komputation.cpu.layers.forward.encoder

import shape.komputation.cpu.functions.add
import shape.komputation.cpu.functions.extractStep
import shape.komputation.cpu.layers.BaseForwardLayer
import shape.komputation.cpu.layers.forward.units.RecurrentUnit
import shape.komputation.matrix.*
import shape.komputation.optimization.Optimizable

class CpuMultiOutputEncoder internal constructor(
    name : String?,
    isReversed : Boolean,
    private val unit: RecurrentUnit,
    private val numberSteps: Int,
    private val inputDimension: Int,
    private val hiddenDimension : Int) : BaseForwardLayer(name), Optimizable {

    private val startAtTheBeginning = 0..numberSteps - 1
    private val startAtTheEnd = this.numberSteps - 1 downTo 0

    private val stepIndices = if(isReversed) IntArray(this.numberSteps) { index -> this.numberSteps - 1 - index } else IntArray(this.numberSteps) { index -> index }

    override fun forward(input: DoubleMatrix, isTraining : Boolean): DoubleMatrix {

        var state = doubleZeroColumnVector(this.hiddenDimension)

        val output = doubleZeroMatrix(this.hiddenDimension, this.numberSteps)

        for (indexStep in this.startAtTheBeginning) {

            val stepInput = input.getColumn(this.stepIndices[indexStep])

            state = this.unit.forwardStep(indexStep, state, stepInput, isTraining)

            output.setColumn(indexStep, state.entries)

        }

        return output

    }

    override fun backward(incoming: DoubleMatrix): DoubleMatrix {

        val seriesBackwardWrtInput = doubleZeroMatrix(this.inputDimension, this.numberSteps)

        var stateChain = EMPTY_DOUBLE_MATRIX

        val incomingEntries = incoming.entries

        for (indexStep in this.startAtTheEnd) {

            val incomingStepEntries = extractStep(incomingEntries, indexStep, this.hiddenDimension)

            val chainEntries =

                if (indexStep + 1 == this.numberSteps) {

                    incomingStepEntries
                }
                else {

                    add(stateChain.entries, incomingStepEntries)

                }

            val (backwardStatePreActivationWrtPreviousState, backwardStatePreActivationWrtInput) = this.unit.backwardStep(indexStep, doubleColumnVector(*chainEntries))

            stateChain = backwardStatePreActivationWrtPreviousState

            seriesBackwardWrtInput.setColumn(this.stepIndices[indexStep], backwardStatePreActivationWrtInput.entries)

        }

        this.unit.backwardSeries()

        return stateChain

    }

    override fun optimize(scalingFactor : Double) {

        if (this.unit is Optimizable) {

            this.unit.optimize(scalingFactor)

        }

    }

}