__device__ float backwardRelu (float forward, float chain)
{

    if(forward > 0.0) {

        return chain;

    }
    else {

        return 0.0;

    }

}

extern "C"
__global__ void backwardReluKernel (int length, float *forward, float *chain, float *destination)
{

    int index = blockDim.x * blockIdx.x + threadIdx.x;

    if(index < length) {

        destination[index] = backwardRelu(forward[index], chain[index]);

    }

}