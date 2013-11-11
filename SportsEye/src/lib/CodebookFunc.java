package lib;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Vector;

import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvFileStorage;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d.BOWKMeansTrainer;
import com.googlecode.javacv.cpp.opencv_features2d.DescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_features2d.KeyPoint;
import com.googlecode.javacv.cpp.opencv_nonfree.SURF;

public class CodebookFunc {

	/** write calculated descriptors into target file
	 * @param fileName (name of target file) 
	 * @param fileSize (number of descriptors written in each file, maximum 200)
	 * @param inputDirectory
	 * @param descriptorOrderPath
	 * @param surf
	 * @param extractor
	 * @param fileInitialNum (index for each file name)
	 */
	
	public void writeDescriptorsIntoFile(String fileName, int fileSize,String inputDirectory, String descriptorOrderPath, SURF surf, 
			DescriptorExtractor extractor, int fileInitialNum){
		
		File file = new File(inputDirectory);
		File[] files = file.listFiles();
		//System.out.println("file size: " + files.length);
		
		Vector<String> filepaths = GeneralFunc.getFilePath(files);
		
		int num = 0;
		File descriptorOrder = new File(descriptorOrderPath);
		if(!descriptorOrder.exists()){
			try {
				descriptorOrder.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		FileWriter fw = null;
		try {
			fw = new FileWriter(descriptorOrder,false); //set true for append
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter writer = new PrintWriter(bw);
		
		String name = fileName + fileInitialNum + ".yml";
		int totalRows = 0;
		
		IplImage image;
		IplImage imageProcessed;
		KeyPoint keypoints = new KeyPoint();
		
		CvFileStorage fs = CvFileStorage.open(name, null, 1);// CV_STORAGE_WRITE=1, READ =0, Append =2
		System.out.println("write descriptors into:" + name);
		
		writer.print(name + ":");
		
		for (int i = 0; i < filepaths.size(); i++){
			String filepath = filepaths.get(i);
			String imageName = filepath.substring(filepath.lastIndexOf("\\")+1, filepath.lastIndexOf(".") );
			image = cvLoadImage(filepath);
			imageProcessed = ImageProcessingFunc.smooth(ImageProcessingFunc.histEqualize(image));
			CvMat descriptor = new CvMat(null);
			
			surf.detect(imageProcessed, null, keypoints);
			extractor.compute(imageProcessed, keypoints, descriptor);
			
			
			totalRows += descriptor.rows();
				
			String nodeName = "training_descriptors_"+ imageName;
				
			opencv_core.cvWrite(fs,nodeName, descriptor);
				
			descriptor.deallocate();
			opencv_core.cvReleaseImage(image);
			opencv_core.cvReleaseImage(imageProcessed);
			System.out.print(".");
			//System.out.println("image " + imageName + " calculated...");
			num ++;
			
			writer.print(nodeName + ",");
				
			if ( i % fileSize == (fileSize -1)){
				opencv_core.cvReleaseFileStorage(fs);
				fileInitialNum += 1;
				fileName = "training_descriptors_" + fileInitialNum + ".yml" ; 
				fs = CvFileStorage.open(fileName, null, 1);
				System.out.println("\nwrite descriptors into: " + fileName);
				writer.println();
				writer.print(fileName + ":");
			}
			
			
		}
		
		opencv_core.cvReleaseFileStorage(fs);
		writer.flush();
		writer.close();
		
		System.out.println("\ntotal rows of descriptors: " + totalRows);
		System.out.println("total samples: " + num);
	}
	
	public void readDescriptorsFromFile(String inputDirectory,final String fileName, String helpFile, BOWKMeansTrainer BOWtrainer){
		
		
		System.out.println("\nload help file...");
		Vector<String> fileContent = new Vector<String>();
		File descriptorOrder = new File(helpFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(descriptorOrder));
			String line = "";
			try {
				while((line = br.readLine()) != null){
					fileContent.add(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println("file content size: " + fileContent.size());
		
		File dir = new File(inputDirectory);
		FilenameFilter filter = new FilenameFilter(){

			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				return name.startsWith(fileName);
			}
		};
		System.out.println("check target .yml file...");
		
		String[] filepaths = dir.list(filter);
		//System.out.println("filepath length: " + filepaths.length);
		if (filepaths.length != 0){
			for(int i = 0; i < filepaths.length; i++){
				 
				String filepath = filepaths[i];
				//System.out.println("file path: " + filepath);
				String name = filepath.substring(filepath.lastIndexOf("\\") +1, filepath.lastIndexOf("."));
				System.out.println("find file " + name);
				for(String content : fileContent){
					//System.out.println("test");
					//System.out.println(name);
					if (content.startsWith(name)){
						
						String descriptorNames = content.substring(content.indexOf(":") + 1);
						String[] descriptorName = descriptorNames.split(",");
						
						for (int z = 0; z < descriptorName.length; z++){
							//System.out.println(descriptorName[z]);
						}
						
						CvFileStorage fs = CvFileStorage.open(filepath, null, 0);
						//System.out.println("\nread file " + name + "...");
						for (int j = 0; j < descriptorName.length; j++){
							String nodeName = descriptorName[j];
							Pointer pointer = opencv_core.cvReadByName(fs, null, nodeName, null);
							System.out.print(".");
							//System.out.println("read descriptor from " + nodeName + "...");
							CvMat descriptors = new CvMat(pointer);
							BOWtrainer.add(descriptors);
						}
						opencv_core.cvReleaseFileStorage(fs);
					}
				}
			} 
		}else{
			System.out.println("there is no such file");
		}
		
		System.out.println("\nfinish loading descriptors:" + new Date());
		CvMat vocabulary = BOWtrainer.cluster();
		System.out.println("finish clustering, vocabulary created: " + new Date());
		
		CvFileStorage fs1 = CvFileStorage.open("vocabulary.yml", null, 1);
		opencv_core.cvWrite(fs1, "vocabulary", vocabulary);
		opencv_core.cvReleaseFileStorage(fs1);
		System.out.println("codebook has been created! ");
	}
}
