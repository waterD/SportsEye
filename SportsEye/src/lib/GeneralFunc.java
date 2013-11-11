package lib;

import java.io.File;
import java.util.Vector;

public class GeneralFunc {

	
	public static Vector<String> getFilePath(File[] files){
		
		Vector<String> filepaths = new Vector<String>();
			
			for(int i = 0; i <files.length; i++){
				String filepath = files[i].getPath();
				filepaths.add(filepath);
				}
			
			return filepaths;
	}

	
	public static void FileSize(String inputDirectory){
		
		int size;
		String path;
		String folderName;
		
		File inputPath = new File(inputDirectory);
		
		File[] files = inputPath.listFiles();
		for (int i = 0; i < files.length; i++){
			if (files[i].isDirectory()){
				File[] subFiles = files[i].listFiles();
				path = files[i].getAbsolutePath();
				folderName = path.substring(path.lastIndexOf('\\')+1);
				size = subFiles.length;
				System.out.println(folderName + ": ");
				System.out.print(size + "\n");
			}
		}
	}
	
	public static String char_to_class(char c){
		switch(c){
		case 'b':
		case 'B':
			return "basketball";
		case 'f':
		case 'F':
			return "football";
		case 'v':
		case 'V':
			return "volleyball";
		default:
			return "";
		}
	
	}	
}
