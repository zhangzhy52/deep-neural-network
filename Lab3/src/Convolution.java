import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.text.Position.Bias;



public class Convolution {

/*
	public void createNextLayer(Layer preLayer ){
		Layer currLayer =  new Layer();
		currLayer.kernelSize = new Layer.Size(5,5);
		currLayer.initKernel();

		currLayer.outputSize = new Layer.Size(preLayer.outputSize.x - currLayer.kernelSize.x + 1,
				preLayer.outputSize.y - currLayer.kernelSize.y + 1);

		currLayer.outMapNum = preLayer.outMapNum * currLayer.kernelNum;

	}
	*/

    public static void forward(Layer prevLayer, Layer currLayer){ // How to update bias?

        int numKernel = currLayer.kernelNum;
        int outputX = currLayer.outputSize.x;
        int outputY = currLayer.outputSize.y;
        for (int i = 0;i < currLayer.outMapNum; i++){
            for (int j = 0; j < currLayer.channelNum; j++){

                currLayer.outMaps[i][j] = convolve(prevLayer.outMaps[i/numKernel][j],
                        currLayer.kernel[i % numKernel][j])  ;
                //True means take ReLu as the activation function.
                matrixAdd(currLayer.outMaps[i][j],
                        createConstantMatrix(currLayer.bias[i % numKernel][j], outputX, outputY), true);
            }
        }

    }
    //public double[][][][] error;   // outMapNum * channelNum *  outputSize.x * outputSize.y
    //public double[][][][] kernel;  // kernelNum * channelNum * kernelSize * kernelSize
    //public double[][] bias; // kernelNum * channelNum;
    public static void backprop(Layer prevLayer, Layer currLayer, double stepSize){

        int numKernel = currLayer.kernelNum;
        int kernelx = currLayer.kernelSize.x;
        int kernely = currLayer.kernelSize.y;

        double[][][][] dKernel = new double[numKernel][currLayer.channelNum][kernelx][kernely]; //store gradient
        double[][] dBias = createConstantMatrix(0.0, numKernel, currLayer.channelNum);

        for (int i = 0; i < numKernel; i++)
            for (int j =0; j < currLayer.channelNum; j++){
                dKernel[i][j] = createConstantMatrix(0.0, kernelx, kernely);
            }

        // update error[][][][] of prevLayer
        for (int i = 0 ; i < currLayer.outMapNum; i++)
            for (int j = 0; j < currLayer.channelNum; j++){
                if (i % numKernel == 0)
                    prevLayer.setErrorZero(i/numKernel, j);



                matrixAdd(prevLayer.error[i/numKernel][j] ,
                        convolve( zeroPadding(currLayer.error[i][j], kernelx, kernely),
                                reverseKernel(currLayer.kernel[i % numKernel][j])),false
                );
                // two ways to calculate, may need debug
                matrixAdd(dKernel[i % numKernel][j] ,
                        convolve (prevLayer.outMaps[i/numKernel][j] , currLayer.error[i][j]),
                        false);

                dBias[i % numKernel][j] += avgMatrix(currLayer.error[i][j]);

            }
        // update kernel[][][][] and bias of currLayer
        for (int i = 0; i < numKernel; i++)
            for (int j = 0; j < currLayer.channelNum; j++){
                constantTimeMatrix(-stepSize, dKernel[i][j]);
                matrixAdd(currLayer.kernel[i][j],dKernel[i][j] , false);

                // "-" if error = currentLabel - trueLabel;
                currLayer.bias[i][j] -=  dBias[i][j]/ prevLayer.outMapNum;
            }
    }


    private static double[][] convolve (double[][]image, double[][]kernel ) // convolution
    {
        int iRow = image.length;
        int iColumn = image[0].length;
        int kRow = kernel.length;
        int kColumn = kernel[0].length;

        double[][] result = new double[iRow - kRow + 1 ][ iColumn - kColumn + 1];
        for (int i = 0; i < iRow - kRow + 1; i++)
            for (int j = 0; j < iColumn - kColumn + 1; j++)
            {
                double tmp = 0;
                for (int x = i; x < i + kRow; x++)
                    for (int y = j; y < j + kColumn; y++)
                    {
                        tmp += image[x][y] * kernel[x - i][y - j];

                    }
                result[i][j] = tmp;

            }
        return result;
    }

    // rotate 180
    private static double[][] reverseKernel(double[][] kernel){
        int row = kernel.length;
        int col = kernel[0].length;
        double[][] reverse = new double[row][col];
        for (int i = 0; i < row; i++)
            for (int j = 0; j< col; j++){
                reverse[i][j] = kernel[row - i - 1][col - j - 1];
            }
        return reverse;
    }

    // zero padding.
    private static double[][] zeroPadding (double[][] matrix, int kernelx, int kernely){
        int row = matrix.length;
        int col = matrix[0].length;
        int newRow = row + 2 * kernelx - 2;
        int newCol = col + 2 * kernely -2;
        double[][] padding = new double[newRow ][newCol];
        for (int i = 0; i < newRow; i++)
            for (int j = 0; j < newCol;j++){
                if(i > kernelx -2 && j > kernely - 2 && i < newRow  - kernelx + 1 && j< newCol -kernely+1)
                    padding[i][j] = matrix[i - kernelx + 1][j - kernely + 1];
                else
                    padding[i][j] = 0;
            }
        return padding;
    }

    // matrix addition
    private static void matrixAdd( double[][] m1, double [][] m2, boolean reLu){
        int row = m1.length;
        int col = m1[0].length;

        for (int i = 0 ; i < row; i++)
            for (int j = 0; j < col; j++)
            {
                m1[i][j] += m2[i][j];
                if (reLu){
                    m1[i][j] = reLu(m1[i][j]);
                }

            }

    }

    private static double[][] createConstantMatrix(double bias, int row, int col)
    {
        double[][] biasM = new double[row][col];
        for (int i = 0; i < row ;i ++)
            for (int j = 0; j< col; j++){
                biasM[i][j] = bias;
            }
        return biasM;
    }

    private static void constantTimeMatrix(double number, double[][] matrix)
    {
        int row = matrix.length;
        int col = matrix[0].length;

        for (int i = 0 ; i < row; i++)
            for (int j = 0; j < col; j++)
                matrix[i][j] *= number;
    }

    private static double avgMatrix(double[][] matrix){
        double sum = 0.0;
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix[0].length; j++)
                sum += matrix[i][j];
        return sum/(matrix.length * matrix[0].length) ;
    }



    private static double sigmoid(double x) {
        return (1/( 1 + Math.pow(Math.E,(-1*x))));
    }
    private static double reLu(double x){
        return x > 0 ? x: -x;
    }



    public static class LayerBuilder {
        private List<Layer> mLayers;

        public LayerBuilder() {
            mLayers = new ArrayList<Layer>();
        }

        public LayerBuilder(Layer layer) {
            this();
            mLayers.add(layer);
        }

        public LayerBuilder addLayer(Layer layer) {
            mLayers.add(layer);
            return this;
        }

    }


}
