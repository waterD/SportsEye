package main;

import separate.CreateCodebook;
import separate.ImagePrediction;
import separate.SVMTraining;

public class SportsEyeMain {

	
	public SportsEyeMain(){
		
		CreateCodebook codebook = new CreateCodebook();
		codebook.createCodebook();
		
		SVMTraining  svm = new SVMTraining();
		svm.training();
		
		ImagePrediction imagePrediction = new ImagePrediction();
		imagePrediction.predict();
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		new SportsEyeMain();
		
	}

}
