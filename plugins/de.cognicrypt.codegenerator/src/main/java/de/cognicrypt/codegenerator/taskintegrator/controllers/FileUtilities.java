/**
 * 
 */
package de.cognicrypt.codegenerator.taskintegrator.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.cognicrypt.codegenerator.Activator;
import de.cognicrypt.codegenerator.Constants;
import de.cognicrypt.codegenerator.question.Answer;
import de.cognicrypt.codegenerator.question.ClaferDependency;
import de.cognicrypt.codegenerator.question.CodeDependency;
import de.cognicrypt.codegenerator.question.Question;
import de.cognicrypt.codegenerator.taskintegrator.models.ClaferModel;
import de.cognicrypt.codegenerator.tasks.Task;
import de.cognicrypt.codegenerator.utilities.Utils;

/**
 * @author rajiv
 *
 */

public class FileUtilities {

	private String taskName;	
	private StringBuilder errors; // Maintain all the errors to display them on the wizard.
	
	/**
	 * The class needs to be initialized with a task name, as it is used extensively in the methods.
	 * 
	 * @param taskName
	 */
	public FileUtilities(String taskName) {
		super();
		this.setTaskName(taskName);
		setErrors(new StringBuilder());
	}

	/**
	 * 
	 * @return the result of the comilation.
	 */
	private boolean compileCFRFile() {
		// try to compile the Clafer file
		// TODO error handling missing
		String claferFilename = Constants.CFR_FILE_DIRECTORY_PATH + getTrimmedTaskName() + Constants.CFR_EXTENSION;
		return ClaferModel.compile(claferFilename);
	}

	/**
	 * Write the data from the pages to target location in the plugin.
	 * @param claferModel
	 * @param questions
	 * @param xslFileContents
	 * @param customLibLocation
	 */
	public String writeFiles(ClaferModel claferModel, ArrayList<Question> questions, String xslFileContents, File customLibLocation) {
		writeCFRFile(claferModel);
		compileCFRFile();
		try {
			writeJSONFile(questions);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeXSLFile(xslFileContents);
		if (customLibLocation != null) {
			copyFileFromPath(customLibLocation);
		}
		return errors.toString();
	}
	
	/**
	 * Copy the selected files to target location in the plugin.
	 * @param cfrFileLocation
	 * @param jsonFileLocation
	 * @param xslFileLocation
	 * @param customLibLocation
	 */
	public String writeFiles(File cfrFileLocation, File jsonFileLocation, File xslFileLocation, File customLibLocation) {
		
		boolean isCFRFileValid = validateCFRFile(cfrFileLocation);
		boolean isJSONFileValid = validateJSONFile(jsonFileLocation);
		boolean isXSLFileValid = validateXSLFile(xslFileLocation);

		if (isCFRFileValid && isJSONFileValid && isXSLFileValid) {

			// custom library location is optional.
			if (customLibLocation != null) {
				if (validateJARFile(customLibLocation)) {
					copyFileFromPath(customLibLocation);
				}
			}

			copyFileFromPath(cfrFileLocation);
			copyFileFromPath(jsonFileLocation);

			String cfrFilename = cfrFileLocation.getAbsolutePath();
			String jsFilename = cfrFilename.substring(0, cfrFilename.lastIndexOf(".")) + Constants.JS_EXTENSION;
			copyFileFromPath(new File(jsFilename));

			copyFileFromPath(xslFileLocation);

		}

		return errors.toString();
	}

	/**
	 * For the sake of reusability.
	 * 
	 * @param fileName
	 */
	private void appendFileErrors(String fileName) {
		errors.append("The contents of the file ");
		errors.append(fileName);
		errors.append(" are invalid.");
		errors.append("\n");
	}

	/**
	 * Validate the provided JAR file before copying it to the target location.
	 * @param customLibLocation
	 * @return a boolean value for the validity of the file.
	 */
	private boolean validateJARFile(File customLibLocation) {
		boolean validFile = true;
		// Loop through the files, since the custom library is a directory.
		if (customLibLocation.isDirectory()) {
			for (File tmpLibLocation : customLibLocation.listFiles()) {
				if (tmpLibLocation.getPath().endsWith(Constants.JAR_EXTENSION)) {
					ZipFile customLib;
					try {
						customLib = new ZipFile(tmpLibLocation);
						Enumeration<? extends ZipEntry> e = customLib.entries();
						customLib.close();
					} catch (IOException ex) {
						ex.printStackTrace();
						appendFileErrors(tmpLibLocation.getName());
						return false;
					}
				}
			}
		}
		return validFile;
	}

	/**
	 * Validate the provided XSL file before copying it to the target location.
	 * @param xslFileLocation
	 * @return a boolean value for the validity of the file.
	 */
	private boolean validateXSLFile(File xslFileLocation) {
		try {
			TransformerFactory.newInstance().newTransformer(new StreamSource(xslFileLocation));			
		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			e.printStackTrace();
			appendFileErrors(xslFileLocation.getName());
			return false;
		}
		return true;
	}

	/**
	 * Validate the provided JSON file before copying it to the target location.
	 * @param jsonFileLocation
	 * @return a boolean value for the validity of the file.
	 */
	private boolean validateJSONFile(File jsonFileLocation) {			
		Gson gson = new Gson();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(jsonFileLocation));
            gson.fromJson(reader, Object.class);
            reader.close();            
            return true;
        } catch (com.google.gson.JsonSyntaxException | IOException ex) {
        	ex.printStackTrace();
			appendFileErrors(jsonFileLocation.getName());
            return false;
        }
	}

	/**
	 * Validate the provided CFR file before copying it to the target location.
	 * @param cfrFileLocation
	 * @return a boolean value for the validity of the file.
	 */
	private boolean validateCFRFile(File cfrFileLocation) {
		boolean compilationResult = ClaferModel.compile(cfrFileLocation.getAbsolutePath());
		if (!compilationResult) {
			appendFileErrors(cfrFileLocation.getName());
			errors.append("Compilation failed.");
			errors.append("\n");
		}
		return compilationResult;
	}

	/**
	 * Copy the given file to the appropriate location. 
	 * @param existingFileLocation
	 */	
	private void copyFileFromPath(File existingFileLocation) {
		if (existingFileLocation.exists() && !existingFileLocation.isDirectory()) {
			File targetDirectory = null;
			try {
				
				if(existingFileLocation.getPath().endsWith(Constants.CFR_EXTENSION)) {
					targetDirectory = new File(Utils.getResourceFromWithin(Constants.CFR_FILE_DIRECTORY_PATH), getTrimmedTaskName() + Constants.CFR_EXTENSION);
				} else if (existingFileLocation.getPath().endsWith(Constants.JS_EXTENSION)) {
					targetDirectory = new File(Utils.getResourceFromWithin(Constants.CFR_FILE_DIRECTORY_PATH), getTrimmedTaskName() + Constants.JS_EXTENSION);
				} else if(existingFileLocation.getPath().endsWith(Constants.JSON_EXTENSION)) {
					targetDirectory = new File(Utils.getResourceFromWithin(Constants.JSON_FILE_DIRECTORY_PATH), getTrimmedTaskName() + Constants.JSON_EXTENSION);
				} else if(existingFileLocation.getPath().endsWith(Constants.XSL_EXTENSION)) {
					targetDirectory = new File(Utils.getResourceFromWithin(Constants.XSL_FILE_DIRECTORY_PATH), getTrimmedTaskName() + Constants.XSL_EXTENSION);
				} else {
					throw new Exception("Unknown file type.");
				}
			
				if (targetDirectory != null) {
					Files.copy(existingFileLocation.toPath(), targetDirectory.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
				}

			} catch (Exception e) {				
				e.printStackTrace();
				errors.append("There was a problem copying file ");
				errors.append(existingFileLocation.getName());
				errors.append("\n");
			}
			// If we are dealing with a custom library location.
		} else if (existingFileLocation.exists() && existingFileLocation.isDirectory()) {
			File tempDirectory = new File(Utils.getResourceFromWithin(Constants.JAR_FILE_DIRECTORY_PATH), getTrimmedTaskName() + Constants.innerFileSeparator);
			tempDirectory.mkdir();
			// Loop through all the containing files.
			for (File customLibFile : existingFileLocation.listFiles()) {
				File tmpFile = new File(tempDirectory.toString() + Constants.SLASH + customLibFile.getName());
				try {
					Files.copy(customLibFile.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
				} catch (IOException e) {
					e.printStackTrace();
					errors.append("There was a problem copying file ");
					errors.append(existingFileLocation.getName());
					errors.append("\n");
				}
			}
		}
	}
	
	/**
	 * Update the task.json file with the new Task.
	 * @param task the Task to be added.
	 */
	public void writeTaskToJSONFile(Task task){
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			reader = new BufferedReader(new FileReader(Utils.getResourceFromWithin(Constants.jsonTaskFile)));
			List<Task> tasks = gson.fromJson(reader, new TypeToken<List<Task>>() {}.getType());	
			// Add the new task to the list.
			tasks.add(task);
			reader.close();
			
			writer = new BufferedWriter(new FileWriter(Utils.getResourceFromWithin(Constants.jsonTaskFile)));			
			gson.toJson(tasks, new TypeToken<List<Task>>() {}.getType(), writer);
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			errors.append("There was a problem updating the task file.\n");
		}
	}
		
	/**
	 * 
	 * @param claferModel
	 */
	private void writeCFRFile(ClaferModel claferModel) {
		File cfrFile = new File(Utils.getResourceFromWithin(Constants.CFR_FILE_DIRECTORY_PATH), getTrimmedTaskName() + Constants.CFR_EXTENSION);
		try {
			FileWriter writer = new FileWriter(cfrFile);
			writer.write(claferModel.toString());
			writer.close();
		} catch (IOException e) {
			Activator.getDefault().logError(e);
			errors.append("There was a problem writing the Clafer model.\n");
		}
	}
		
	/**
	 * 
	 * @param questions
	 * 			listOfAllQuestions
	 * @throws IOException 
	 */
	private void writeJSONFile(ArrayList<Question> questions) throws IOException {
		System.out.println(questions.size());

		File jsonFileTargetDirectory = new File(Utils.getResourceFromWithin(Constants.JSON_FILE_DIRECTORY_PATH), getTaskName() + Constants.JSON_EXTENSION);
		
		//creates the file
		jsonFileTargetDirectory.createNewFile();
		
		/*
		 * In following StringBuilder object all the informations required for creating the
		 * json file is appended 
		 */
		StringBuilder sb = new StringBuilder();
		
		sb.append(Constants.openSquareBracket + Constants.openCurlyBrace + Constants.lineSeparator);
		
		sb.append(Constants.quotationMark + Constants.taskIDField + Constants.quotationMark + Constants.colonOperator + " " + 
			Constants.quotationMark + Constants.taskIDValue + Constants.quotationMark);
		
		sb.append(Constants.lineSeparator);
		
		sb.append(Constants.quotationMark+Constants.helpIDField+Constants.quotationMark+Constants.colonOperator+" "+
		Constants.quotationMark + taskName + "_Page0"+Constants.quotationMark);
		
		sb.append(Constants.lineSeparator);
	
		sb.append(Constants.quotationMark+Constants.contentFieldName+Constants.quotationMark+Constants.colonOperator+" "+Constants.openSquareBracket);
		
		//Counter used for creating the question json object 
		int moreQuestions=0;
		for(Question question: questions){
			sb.append(Constants.openCurlyBrace+Constants.lineSeparator);
			sb.append(Constants.quotationMark+Constants.qstnIDField+Constants.quotationMark+Constants.colonOperator+" ");
			sb.append(Constants.quotationMark+question.getId()+Constants.quotationMark+Constants.commaOperator+Constants.lineSeparator);
			sb.append(Constants.quotationMark+Constants.elementField+Constants.quotationMark+Constants.colonOperator+" "+
			Constants.quotationMark+question.getQuestionType()+Constants.quotationMark+Constants.commaOperator+Constants.lineSeparator);
			sb.append(Constants.quotationMark+Constants.noteField+Constants.quotationMark+Constants.colonOperator+" "+Constants.quotationMark+" "+Constants.quotationMark
				+Constants.commaOperator+Constants.lineSeparator);
			sb.append(Constants.quotationMark+Constants.qstnTxtField+Constants.quotationMark+Constants.colonOperator+" "+
				Constants.quotationMark+question.getQuestionText()+Constants.quotationMark+Constants.commaOperator+Constants.lineSeparator);
			sb.append(Constants.quotationMark+Constants.answersField+Constants.quotationMark+Constants.colonOperator+" "+
				Constants.openSquareBracket);
			
		//to be use for creating the array of answersdetails as json object
			int i=0;
			for (Answer answer : question.getAnswers()) {
				sb.append(Constants.openCurlyBrace+Constants.lineSeparator);
				sb.append(Constants.quotationMark + Constants.valueField + Constants.quotationMark + Constants.colonOperator + " " + Constants.quotationMark + answer
					.getValue() + Constants.quotationMark);
				//Executes when clafer dependency list is not empty
				int claferCounter=0;
				if (answer.getClaferDependencies() != null) {
					sb.append(Constants.commaOperator + Constants.lineSeparator+Constants.quotationMark + Constants.claferDependenciesField + Constants.quotationMark + Constants.colonOperator + " " + Constants.openSquareBracket );
					for (ClaferDependency cd : answer.getClaferDependencies()) {
						claferCounter++;
						sb.append(Constants.openCurlyBrace + Constants.lineSeparator);
						sb.append(Constants.quotationMark + Constants.algorithmField + Constants.quotationMark + Constants.colonOperator + " " + Constants.quotationMark + cd
							.getAlgorithm() + Constants.quotationMark + Constants.commaOperator + Constants.lineSeparator);
						sb.append(Constants.quotationMark + Constants.operandField + Constants.quotationMark + Constants.colonOperator + " " + Constants.quotationMark + cd
							.getOperand() + Constants.quotationMark + Constants.commaOperator + Constants.lineSeparator);
						sb.append(Constants.quotationMark + Constants.valueField + Constants.quotationMark + Constants.colonOperator + " " + Constants.quotationMark + cd
							.getValue() + Constants.quotationMark + Constants.commaOperator + Constants.lineSeparator);
						sb.append(Constants.quotationMark + Constants.operatorField + Constants.quotationMark + Constants.colonOperator + " " + Constants.quotationMark + cd
							.getOperator() + Constants.quotationMark + Constants.lineSeparator);
						sb.append(Constants.closeCurlyBrace);
						if(answer.getClaferDependencies().size()<claferCounter){
							sb.append(Constants.commaOperator);
						}
					}
					sb.append(Constants.closeSquareBracket );
				}
				
				//Executes when code dependency list is not empty
				int codeCounter=0;
				if(answer.getCodeDependencies()!=null){
					
					sb.append(Constants.commaOperator+Constants.lineSeparator+Constants.quotationMark+Constants.codeDependenciesField+Constants.quotationMark+Constants.colonOperator+" "+
						Constants.openSquareBracket);
					for(CodeDependency cd: answer.getCodeDependencies()){
						codeCounter++;
						sb.append(Constants.openCurlyBrace+Constants.lineSeparator);
						sb.append(Constants.quotationMark+Constants.optionField+Constants.quotationMark+Constants.colonOperator+" "+
						Constants.quotationMark+cd.getOption()+Constants.quotationMark+Constants.commaOperator+Constants.lineSeparator);
					sb.append(Constants.quotationMark+Constants.valueField+Constants.quotationMark+Constants.colonOperator+""+
						Constants.quotationMark+cd.getValue()+Constants.quotationMark+Constants.lineSeparator+Constants.closeCurlyBrace);
					
					if(answer.getCodeDependencies().size()<codeCounter){
						sb.append(Constants.commaOperator);
					}
					}
				sb.append(Constants.closeSquareBracket);
				}
				
				//checks if current answer is default or not
				if(answer.isDefaultAnswer()){
					sb.append(Constants.commaOperator+Constants.lineSeparator+Constants.quotationMark+Constants.defaultAnswerField+Constants.quotationMark+Constants.colonOperator+" "+
						Constants.quotationMark+"true");
				}
				
				//checks if answer is linked to other question
				if(answer.getNextID()!=-2){
				sb.append(Constants.commaOperator+Constants.lineSeparator+Constants.quotationMark+Constants.nextIDField+Constants.quotationMark+Constants.colonOperator+" "+
					Constants.quotationMark+answer.getNextID()+Constants.quotationMark+Constants.lineSeparator);
				}
				sb.append(Constants.closeCurlyBrace);
				i++;
				//checks if more answers are there to be added 
				if(question.getAnswers().size()>i){
					sb.append(Constants.commaOperator+Constants.lineSeparator);
				}
			}
			
			sb.append(Constants.closeSquareBracket+Constants.lineSeparator+Constants.closeCurlyBrace);
			moreQuestions++;
			//checks if there are more questions to be added 
			if(questions.size()>moreQuestions){
				sb.append(Constants.commaOperator+Constants.lineSeparator);
			}
			
		}
		sb.append(Constants.closeSquareBracket+Constants.lineSeparator+Constants.closeCurlyBrace+Constants.closeSquareBracket);
		
		//creates the writer object for json file  
		FileWriter writerForJsonFile = new FileWriter(jsonFileTargetDirectory);
		String jsonData= sb +"";
		
		try{
		//write the data into the .json file  
			writerForJsonFile.write(jsonData);
		}
		finally{
		writerForJsonFile.flush();
		writerForJsonFile.close();
		}

		/*//creates a FileReader object for json file
		FileReader readerForJsonFile = new FileReader(jsonFileTargetDirectory);
		char[] r = new char[10];
		readerForJsonFile.read(r);
		
		for(char a : r ){
			System.out.println(a);
			readerForJsonFile.close();
		}
		System.out.println(questions.size());*/
	}
	
	/**
	 * 
	 * @param xslFileContents
	 */
	private void writeXSLFile(String xslFileContents) {
		File xslFile = new File(Utils.getResourceFromWithin(Constants.XSL_FILE_DIRECTORY_PATH), getTrimmedTaskName() + Constants.XSL_EXTENSION);
		
		try {
			PrintWriter writer = new PrintWriter(xslFile);
			writer.println(xslFileContents);
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			errors.append("There was a problem wrting the XSL data.\n");
		}
		
		if (!validateXSLFile(xslFile)) {
			xslFile.delete();
			errors.append("The XSL data is invalid.\n");
		}
	}
	
	/**
	 * Return the name of that task that is set for the file writes..
	 * @return
	 */
	private String getTaskName() {
		return taskName;
	}
	
	/**
	 * get machine-readable task name
	 * 
	 * @return task name without non-alphanumerics
	 */
	private String getTrimmedTaskName() {
		return getTaskName().replaceAll("[^A-Za-z0-9]", "");
	}

	/**
	 * 
	 * Set the name of the task that is being written to File. The names of the result files are set based on the provided task name.
	 * @param taskName
	 */
	private void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	/**
	 * @return the list of errors.
	 */
	public StringBuilder getErrors() {
		return errors;
	}

	/**
	 * @param set
	 *        the string builder to maintain the list of errors.
	 */
	public void setErrors(StringBuilder errors) {
		this.errors = errors;
	}

}
