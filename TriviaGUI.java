package il.ac.tau.cs.sw1.trivia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TriviaGUI {

	private static final int MAX_ERRORS = 3;
	private Shell shell;
	private Label scoreLabel;
	private Composite questionPanel;
	private Label startupMessageLabel;
	private Font boldFont;
	private String lastAnswer;
	private List<List<String>> questions = new ArrayList<>();
	private int flagWrongAnswer=0;
	private int index = 0;
	private int numAnswers = 0;
	private boolean isFirstPass = true;
	private boolean isFirst5050 = true;
	private boolean enablePass = true;
	private boolean enable5050 = true;
	
	// Currently visible UI elements.
	Label instructionLabel;
	Label questionLabel;
	private List<Button> answerButtons = new LinkedList<>();
	private Button passButton;
	private Button fiftyFiftyButton;

	

	public void open() {
		createShell();
		runApplication();
	}

	/**
	 * Creates the widgets of the application main window
	 */
	private void createShell() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Trivia");

		// window style
		Rectangle monitor_bounds = shell.getMonitor().getBounds();
		shell.setSize(new Point(monitor_bounds.width / 3,
				monitor_bounds.height / 4));
		shell.setLayout(new GridLayout());

		FontData fontData = new FontData();
		fontData.setStyle(SWT.BOLD);
		boldFont = new Font(shell.getDisplay(), fontData);

		// create window panels
		createFileLoadingPanel();
		createScorePanel();
		createQuestionPanel();
	}

	/**
	 * Creates the widgets of the form for trivia file selection
	 */
	private void createFileLoadingPanel() {
		final Composite fileSelection = new Composite(shell, SWT.NULL);
		fileSelection.setLayoutData(GUIUtils.createFillGridData(1));
		fileSelection.setLayout(new GridLayout(4, false));

		final Label label = new Label(fileSelection, SWT.NONE);
		label.setText("Enter trivia file path: ");

		// text field to enter the file path
		final Text filePathField = new Text(fileSelection, SWT.SINGLE
				| SWT.BORDER);
		filePathField.setLayoutData(GUIUtils.createFillGridData(1));

		// "Browse" button
		final Button browseButton = new Button(fileSelection, SWT.PUSH);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new  SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Button) {
					String path = GUIUtils.getFilePathFromFileDialog(shell);
					if (path!=null)
						filePathField.setText(path);
			}
		}
		});
		
		// "Play!" button
		final Button playButton = new Button(fileSelection, SWT.PUSH);
		playButton.setText("Play!");
		playButton.addSelectionListener(new  SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Button) {
					questions = new ArrayList<>(); //Initializing questions 
					index = 0; 
					scoreLabel.setText("0");
					numAnswers = 0;
					isFirstPass = true;
					isFirst5050 = true;
					String message = "Trivia file format error: Trivia" +
							 " file row must containg a question and four "
							 + "answers, seperated by tabs. (row ";
					String path = filePathField.getText();
					int i=1;
					try (BufferedReader reader = new BufferedReader(
							new FileReader(path));) {
						String line = null;
						while ((line = reader.readLine()) != null) {
							String[] split = line.split("\t");
							if (split.length<5) {
								GUIUtils.showErrorDialog(shell,message +i+")");
								return;
							}
							List<String> answers = new ArrayList<>();
							for (String s: split)
								answers.add(s);
							questions.add(answers);
							i++;
						}
						
						Collections.shuffle(questions);
						getNextQuestion();
					} catch (FileNotFoundException e1) {
						GUIUtils.showErrorDialog(shell,message +i+")");
						
					} catch (IOException e1) {
						GUIUtils.showErrorDialog(shell,message +i+")");
					}
			}
		}
		});
		
	}
	/**
	 * Creates the panel that displays the current score
	 */
	private void createScorePanel() {
		Composite scorePanel = new Composite(shell, SWT.BORDER);
		scorePanel.setLayoutData(GUIUtils.createFillGridData(1));
		scorePanel.setLayout(new GridLayout(2, false));

		final Label label = new Label(scorePanel, SWT.NONE);
		label.setText("Total score: ");

		// The label which displays the score; initially empty
		scoreLabel = new Label(scorePanel, SWT.NONE);
		scoreLabel.setLayoutData(GUIUtils.createFillGridData(1));
	}

	/**
	 * Creates the panel that displays the questions, as soon as the game
	 * starts. See the updateQuestionPanel for creating the question and answer
	 * buttons
	 */
	private void createQuestionPanel() {
		questionPanel = new Composite(shell, SWT.BORDER);
		questionPanel.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));
		questionPanel.setLayout(new GridLayout(2, true));

		// Initially, only displays a message
		startupMessageLabel = new Label(questionPanel, SWT.NONE);
		startupMessageLabel.setText("No question to display, yet.");
		startupMessageLabel.setLayoutData(GUIUtils.createFillGridData(2));
	}

	/**
	 * Serves to display the question and answer buttons
	 */
	private void updateQuestionPanel(String question, List<String> answers) {
		// Save current list of answers.
		List<String> currentAnswers = answers;
		String correctAnswer = currentAnswers.get(0);
		
		// clear the question panel
		Control[] children = questionPanel.getChildren();
		for (Control control : children) {
			control.dispose();
		}

		// create the instruction label
		instructionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		if (lastAnswer!=null)
			instructionLabel.setText(lastAnswer + "Answer the following question:");
		instructionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the question label
		questionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		questionLabel.setText(question);
		questionLabel.setFont(boldFont);
		questionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		answerButtons.clear();
		Collections.shuffle(answers);
		// create the answer buttons
		for (int i = 0; i < 4; i++) {
			Button answerButton = new Button(questionPanel, SWT.PUSH | SWT.WRAP);
			answerButton.setText(answers.get(i));
			GridData answerLayoutData = GUIUtils.createFillGridData(1);
			answerLayoutData.verticalAlignment = SWT.FILL;
			answerButton.setLayoutData(answerLayoutData);
			answerButtons.add(answerButton);

			answerButton.addSelectionListener(new  SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource() instanceof Button) {
						Button b = (Button) e.getSource();
						String answer = b.getText();
						 numAnswers++;
						if (answer.equals(correctAnswer)) {
							updateScorePanel(3);
							flagWrongAnswer = 0;
							lastAnswer = "Correct! ";
						}
						else {
							updateScorePanel(-2);
							flagWrongAnswer += 1;
							lastAnswer = "Wrong... ";
						}			
						getNextQuestion();
				}
			}
			});
			
		}
		
		
		
		// create the "Pass" button to skip a question
		passButton = new Button(questionPanel, SWT.PUSH);
		passButton.setText("Pass");
		GridData data = new GridData(GridData.END, GridData.CENTER, true,
				false);
		data.horizontalSpan = 1;
		passButton.setLayoutData(data);
		if (!enablePass)
			passButton.setEnabled(false);
		passButton.addSelectionListener(new  SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Button) {
					if (isFirstPass) {
						isFirstPass = false;
						updateScorePanel(0); //changes enable if necessary 
						getNextQuestion();
					}
					else if(Integer.parseInt(scoreLabel.getText())>0) {
						updateScorePanel(-1);
						getNextQuestion();
					}
						
			}
		}
		});
		
		
		// create the "50-50" button to show fewer answer options
		fiftyFiftyButton = new Button(questionPanel, SWT.PUSH);
		fiftyFiftyButton.setText("50-50");
		data = new GridData(GridData.BEGINNING, GridData.CENTER, true,
				false);
		data.horizontalSpan = 1;
		fiftyFiftyButton.setLayoutData(data);
		if (!enable5050)
			fiftyFiftyButton.setEnabled(false);
		fiftyFiftyButton.addSelectionListener(new  SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Button) {
					if (isFirst5050) {
						isFirst5050 = false;
						updateScorePanel(0);
						click5050(correctAnswer,answers);
					}
					else if(Integer.parseInt(scoreLabel.getText())>0) {
						updateScorePanel(-1);
						click5050(correctAnswer,answers);
					}
					
						
			}
		}
		});

		// two operations to make the new widgets display properly
		questionPanel.pack();
		questionPanel.getParent().layout();
	}
	
	
	private void click5050(String currectAnswer, List<String> answers) {
		int currectIndex = -1;
		Random r = new Random();
		for (int i=0;i<4;i++) {
			if (answers.get(i).equals(currectAnswer)) {
				currectIndex = i;
				break;
			}
		}
		
		int indexQuestionTokeep = currectIndex;
		while (indexQuestionTokeep==currectIndex) {
			indexQuestionTokeep = r.nextInt(4);
		}
		
		for (int i=0;i<4;i++) {
			if (i!=currectIndex && i!=indexQuestionTokeep)
				answerButtons.get(i).setEnabled(false);
		}
		fiftyFiftyButton.setEnabled(false);
		
	}

	/**
	 * Opens the main window and executes the event loop of the application
	 */
	private void runApplication() {
		shell.open();
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		boldFont.dispose();
	}
	
	private void getNextQuestion() {
		if (flagWrongAnswer>=MAX_ERRORS|| index>=questions.size()) {
			GUIUtils.showInfoDialog(shell, "GAME OVER", "your final score is "+scoreLabel.getText()
					+" after "+numAnswers+" questions");
			index = 0;
			flagWrongAnswer = 0;
			scoreLabel.setText("0");
			isFirstPass = true;
			isFirst5050 = true;
			return;
		}		
		List<String> answers = questions.get(index);
		String question = answers.get(0);
		updateQuestionPanel(question,answers.subList(1, 5));
		index++;
	}
	
	private void updateScorePanel(int score) {
		String s = scoreLabel.getText();
		int newScore = score;
		if (!s.equals(""))
			newScore += Integer.parseInt(s);
		scoreLabel.setText(""+newScore);
		if (newScore<=0 && !isFirstPass) 
			enablePass = false;
		if (newScore<=0 && !isFirst5050)
			enable5050 = false;
		if (newScore>0) {
			enablePass = true;
			enable5050 = true;
		}		
	}
	
	
}
