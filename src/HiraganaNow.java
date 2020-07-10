import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.*;

class HiraganaNow implements KeyListener {
	static final String title = "Hiragana Now - v1.1";
	
	LinkedList<Kana> currentKanaList = new LinkedList<Kana>();
	LinkedList<Kana> remainingKanaList = new LinkedList<Kana>();
	LinkedList<Kana> kanaLineupThisLevel = new LinkedList<Kana>();
	LinkedList<Kana> failedKanaList = new LinkedList<Kana>();
	Kana currentKana = null;
	
	String hpPip = "[]";
	int hp = 0;
	int startingHP = 10;
	String passPip = "[?]";
	int passes = 0;
	int startingPasses = 3;
	int progress = 0;
	int level = 0;
	int maxLevel = 0;
	int hpAtLevelUp = 0;
	int passesAtLevelUp = 0;
	int freePassesUsed = 0;
	int nonFreePassesUsed = 0; 
	int newKanaToAdd = 0;
	
	boolean isThisTheFinalLevel = false;
	boolean inputLock = false;
	boolean waitingForRestart = false;
	JPanel infoPanel = new JPanel();
	JLabel hpLabel = new JLabel();
	JLabel passesLabel = new JLabel();
	JLabel progressLabel = new JLabel();
	JLabel levelLabel = new JLabel();
	JFrame window = new JFrame(title) {
		private static final long serialVersionUID = 1L;
		
		@Override
		public void paint(Graphics g) {
			Toolkit.getDefaultToolkit().sync();
			super.paint(g);
		};
	};
	JLabel kanaLabel = new JLabel();
	JTextField inputField = new JTextField();
	
	Font kanaFont, labelFont;
	
	public static void main(String[] args) {
		new HiraganaNow();
	}
	
	public static <X> X removeRandom(LinkedList<X> list){
		return list.remove((int)(Math.random()*list.size()));
	}
	
	public static <X> void shuffle(LinkedList<X> listToShuffle){
		LinkedList<X> shuffledList = new LinkedList<X>();
		while(!listToShuffle.isEmpty()){
			shuffledList.add(removeRandom(listToShuffle));
		}
		listToShuffle.addAll(shuffledList);
	}
	
	HiraganaNow(){
		window.setLayout(new BorderLayout());
		window.add(infoPanel, BorderLayout.NORTH);
		window.add(kanaLabel, BorderLayout.CENTER);
		window.add(inputField, BorderLayout.SOUTH);
		
		infoPanel.setLayout(new GridLayout(4, 1));
		infoPanel.add(hpLabel);
		infoPanel.add(passesLabel);
		infoPanel.add(levelLabel);
		infoPanel.add(progressLabel);
		
		labelFont = kanaLabel.getFont();
		labelFont = new Font(labelFont.getName(), Font.BOLD, 16);
		kanaFont = new Font(labelFont.getName(), Font.PLAIN, 128);
		
		hpLabel.setFont(labelFont);
		passesLabel.setFont(labelFont);
		progressLabel.setFont(labelFont);
		levelLabel.setFont(labelFont);
		inputField.setFont(labelFont);
		inputField.setHorizontalAlignment(JTextField.CENTER);
		inputField.addKeyListener(this);
		kanaLabel.setFont(kanaFont);
		kanaLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(300, 300);
		window.setLocationRelativeTo(null);
		window.setResizable(false);
		
		restartGame();
	}
	
	void chooseMode(){
		boolean reshowOptions;
		do {
			reshowOptions = false;
			String message = 
				"Choose a mode:";
			String[] options = {"Hiragana Mode", "Katakana Mode", "Instructions"};
			int choice = JOptionPane.showOptionDialog(null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
			switch (choice) {
			case JOptionPane.CLOSED_OPTION:
				System.exit(0);
				break;
			case 0: // Hiragana Mode //
				Kana.loadHiragana();
				break;
			case 1: // Katakana Mode //
				Kana.loadKatakana();
				break;
			case 2: // Instructions //
				String instructionMessage = 
					"Enter the character's romanisation, e.g. \"tsu\" for \"つ\", then press enter.\n"+
					"Enter \"?\" to use a pass: this shows you the answer, but you have a limited supply.\n" +
					"If you enter the wrong romanisation, you lose HP. Once your HP runs out, you lose.\n" +
					"The better you do, the faster your level increases. Get to the final level!\n" +
					"\n" +
					"Send feedback to cheeseybobdev@gmail.com";
				JOptionPane.showMessageDialog(null, instructionMessage, "Instructions - "+title, JOptionPane.PLAIN_MESSAGE);
				reshowOptions = true;
				break;
			}
		} while(reshowOptions);
	}
	
	String hpText(int n){
		String hpText = "";
		for(int i = 0; i < n; i ++){
			hpText = hpText+hpPip;
		}
		return hpText;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
			if(waitingForRestart){
				restartGame();
			} else {
				int choice = JOptionPane.showConfirmDialog(null, "Restart the game?", "", JOptionPane.YES_NO_OPTION);
				if(choice == JOptionPane.YES_OPTION){
					restartGame();
				}
			}
			return;
		}
		if(!inputLock){
			if(e.getKeyCode() == KeyEvent.VK_ENTER){
				test();
				refreshLabelText();
			}
		}
		if(waitingForRestart && e.getKeyCode() == KeyEvent.VK_ENTER){
			waitingForRestart = false;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		// Unused method . //
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		// Unused method . //
	}
	
	void levelUp() {
		// HP //
		hpAtLevelUp = hp < startingHP ? 1 : 0;
		
		// Passes //
		passesAtLevelUp = passes < startingPasses ? 1 : 0;
		
		// New Kana //
		int unusedFreePasses = newKanaToAdd - freePassesUsed;
		newKanaToAdd = Math.max(2, newKanaToAdd + 1 - nonFreePassesUsed + unusedFreePasses);
		freePassesUsed = 0;
		nonFreePassesUsed = 0;
		progress = 0;
		
		if(isThisTheFinalLevel){
			new GameWinEffect();
		} else {
			new LevelUpEffect();
		}
	}
	
	void newGame() {
		currentKanaList.clear();
		remainingKanaList.clear();
		remainingKanaList.addAll(Kana.fullList);
		kanaLineupThisLevel.clear();
		failedKanaList.clear();
		hp = startingHP;
		passes = startingPasses;
		freePassesUsed = 0;
		nonFreePassesUsed = 0;
		newKanaToAdd = 2;
		progress = 0;
		level = 1;
		maxLevel = Kana.fullList.size();
		isThisTheFinalLevel = false;
		nextLevel();
		nextCharacter();
	}
	
	void nextCharacter() {
		if(kanaLineupThisLevel.isEmpty()){
			levelUp();
		} else {
			currentKana = kanaLineupThisLevel.remove();
			kanaLabel.setText(currentKana.character);
			inputField.setText("");
			progress ++;
			new FlashEffect(progressLabel, Color.YELLOW, 3);
		}
		refreshLabelText();
	}
	
	void nextLevel() {
		for(int i = 0; i < newKanaToAdd; i ++){
			if(!remainingKanaList.isEmpty()){
				currentKanaList.add(removeRandom(remainingKanaList));
			} else {
				isThisTheFinalLevel = true;
				break;
			}
		}
		level = currentKanaList.size();
		
		if(isThisTheFinalLevel){
			resetFinalLevel();
		} else {
			// Add a copies of each hiragana to the lineup in a random order, twice. //
			for(int i = 0; i < 2; i ++){
				shuffle(currentKanaList);
				kanaLineupThisLevel.addAll(currentKanaList);
			}
			
			// Add extra copies of hiragana that the player failed previously. //
			while(!failedKanaList.isEmpty()){
				Kana extraHira = removeRandom(failedKanaList);
				int index = (int)(Math.random()*kanaLineupThisLevel.size());
				kanaLineupThisLevel.add(index, extraHira);
			}
			
			// Swap out any doubles. //
			ListIterator<Kana> li = kanaLineupThisLevel.listIterator();
			while(li.hasNext()){
				Kana h1 = li.next();
				if(!li.hasNext()){
					break;
				}
				Kana h2 = li.next();
				// If h1 and h2 are the same, we swap h2 with its successor, if possible. //
				if(h1.equals(h2) && li.hasNext()){
					// Remove h2 from the list. //
					li.remove();
					// Move cursor to the right of h2's successor (now h1's successor). //
					li.next();
					// Add h2 back in to the list. //
					li.add(h2);
				} else {
					// Move back a step so we can continue by checking h2 and h2.next. //
					li.previous();
				}
			}
		}
	}
	
	String passesText(int n) {
		String passesText = "";
		for(int i = 0; i < n; i ++){
			passesText = passesText+passPip;
		}
		if(currentKana.isNewToPlayer){
			passesText = passesText+" (FREE)";
		}
		return passesText;
	}
	
	String progressText(){
		return progress+" / "+(progress+kanaLineupThisLevel.size());
	}
	
	void refreshLabelText(){
		hpLabel.setText(" HP: "+hpText(hp));
		passesLabel.setText(" Passes: "+passesText(passes));
		progressLabel.setText(" Progress: "+progressText());
		levelLabel.setText(" Level: "+level+" / "+maxLevel);
	}
	
	void restartGame() {
		window.setVisible(false);
		chooseMode();
		newGame();
		window.setVisible(true);
	}
	
	void resetFinalLevel(){
		// One copy of each hiragana goes in the lineup, in a random order. //
		kanaLineupThisLevel.clear();
		shuffle(currentKanaList);
		kanaLineupThisLevel.addAll(currentKanaList);
		progress = 0;
	}
	
	void setLock(boolean lock){
		inputLock = lock;
		inputField.setEditable(!lock);
	}
	
	void test() {
		// Retrieve the player's input. //
		String input = inputField.getText();
		
		// A question mark activates a pass. //
		if(input.equals("?")){
			if(currentKana.isNewToPlayer){
				currentKana.isNewToPlayer = false;
				freePassesUsed ++;
				usePass();
			} else if(passes > 0){
				passes --;
				nonFreePassesUsed ++;
				usePass();
			} else {
				new FlashEffect(passesLabel, Color.RED, 5);
				inputField.setText("OUT OF PASSES");
			}
			return;
		}
		
		// Ignore inputs which aren't romaji for any kana. //
		if(!Kana.isValidRomaji(input)){
			new FlashEffect(inputField, Color.RED, 5);
			return;
		}
		
		// Check whether the input is correct. //
		if(input.equals(currentKana.romaji)){
			currentKana.isNewToPlayer = false;
			inputField.setText("");
			new SuccessEffect();
		} else {
			hp --;
			inputField.setText("");
			failedKanaList.add(currentKana);
			new FlashEffect(hpLabel, Color.RED, 5);
			new FailEffect();
			
			// Failure resets the final level marathon. //
			if(isThisTheFinalLevel){
				resetFinalLevel();
			}
		}
	}
	
	void usePass(){
		inputField.setText("");
		failedKanaList.add(currentKana);
		new FlashEffect(passesLabel, Color.YELLOW, 3);
		new PassEffect();
		
		// Using a pass resets the final level marathon. //
		if(isThisTheFinalLevel){
			resetFinalLevel();
		}
	}
	
	abstract class Effect implements Runnable {
		
		Effect(){
			Thread thread = new Thread(this);
			thread.start();
		}
		
		@Override
		public abstract void run();
		
		protected void sleep(long millis){
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	class FailEffect extends Effect {
		@Override
		public void run() {
			setLock(true);
			for(int i = 0; i < 3; i ++){
				kanaLabel.setText(" "+currentKana.character);
				sleep(30);
				kanaLabel.setText(currentKana.character+" ");
				sleep(30);
			}
			sleep(60);
			kanaLabel.setText(currentKana.character);
			if(hp <= 0){
				new GameOverEffect();
			}
			setLock(false);
		}
	}
	
	class FlashEffect extends Effect {
		Component component;
		Color flashColor;
		int numberOfFlashes;
		
		FlashEffect(Component component, Color flashColor, int numberOfFlashes){
			this.component = component;
			this.flashColor = flashColor;
			this.numberOfFlashes = numberOfFlashes;
		}
		
		@Override
		public void run() {
			for(int i = 0; i < numberOfFlashes; i ++){
				component.setForeground(flashColor);
				sleep(100);
				component.setForeground(Color.BLACK);
				sleep(100);
			}
		}
	}
	
	class GameOverEffect extends Effect {
		@Override
		public void run() {
			setLock(true);
			waitingForRestart = true;
			inputField.setText("You'll do better next time!");
			boolean tick = false;
			while(waitingForRestart){
				kanaLabel.setText(tick ? currentKana.character : currentKana.romaji);
				sleep(1000);
				tick = !tick;
			}
			inputField.setText("");
			newGame();
			setLock(false);
		}
	}
	
	class GameWinEffect extends Effect {
		@Override
		public void run() {
			setLock(true);
			waitingForRestart = true;
			inputField.setText("You win!");
			kanaLabel.setText("GREAT SUCCESS");
			kanaLabel.setFont(new Font(kanaFont.getName(), Font.PLAIN, 32));
			while(waitingForRestart){
				kanaLabel.setForeground(Color.YELLOW);
				sleep(100);
				kanaLabel.setForeground(Color.BLACK);
				sleep(100);
			}
			kanaLabel.setFont(kanaFont);
			inputField.setText("");
			newGame();
			setLock(false);
		}
	}
	
	class LevelUpEffect extends Effect {
		@Override
		public void run() {
			setLock(true);
			nextLevel();
			refreshLabelText();
			new FlashEffect(levelLabel, Color.YELLOW, 4);
			
			String levelupTest = isThisTheFinalLevel ? "FINAL LEVEL" : "LEVEL UP";
			int fontSize = isThisTheFinalLevel ? 36 : 48;
			int numberOfFlashes = isThisTheFinalLevel ? 10 : 4;
			kanaLabel.setText(levelupTest);
			kanaLabel.setFont(new Font(kanaFont.getName(), Font.PLAIN, fontSize));
			for(int i = 0; i < numberOfFlashes; i ++){
				kanaLabel.setForeground(Color.YELLOW);
				sleep(100);
				kanaLabel.setForeground(Color.BLACK);
				sleep(100);
			}
			
			if(hpAtLevelUp > 0){
				kanaLabel.setText("");
				sleep(500);
				
				hp += hpAtLevelUp;
				refreshLabelText();
				new FlashEffect(hpLabel, Color.YELLOW, 4);
				String hpText = hpText(hpAtLevelUp);
				kanaLabel.setText(hpText);
				for(int i = 0; i < 4; i ++){
					kanaLabel.setForeground(Color.YELLOW);
					sleep(100);
					kanaLabel.setForeground(Color.BLACK);
					sleep(100);
				}
				hpAtLevelUp = 0;
			}
			
			if(passesAtLevelUp > 0){
				kanaLabel.setText("");
				sleep(500);
				
				passes += passesAtLevelUp;
				refreshLabelText();
				new FlashEffect(passesLabel, Color.YELLOW, 4);
				String passesText = passesText(passesAtLevelUp);
				kanaLabel.setText(passesText);
				for(int i = 0; i < 4; i ++){
					kanaLabel.setForeground(Color.YELLOW);
					sleep(100);
					kanaLabel.setForeground(Color.BLACK);
					sleep(100);
				}
				passesAtLevelUp = 0;
			}
			
			nextCharacter();
			kanaLabel.setFont(kanaFont);
			kanaLabel.setText(currentKana.character);
			setLock(false);
		}
	}
	
	class PassEffect extends Effect {
		@Override
		public void run() {
			setLock(true);
			kanaLabel.setText("["+currentKana.character+"]");
			sleep(500);
			inputField.setText(currentKana.romaji);
			sleep(500);
			kanaLabel.setText("["+currentKana.character+"]");
			sleep(500);
			kanaLabel.setText(currentKana.character);
			setLock(false);
		}
	}
	
	class SuccessEffect extends Effect {
		@Override
		public void run() {
			setLock(true);
			int size = kanaFont.getSize();
			for(int i = 0; i < 10; i ++){
				size = Math.round(size*0.5f);
				kanaLabel.setFont(new Font(kanaFont.getName(), Font.PLAIN, size));
				sleep(30);
			}
			sleep(60);
			kanaLabel.setFont(kanaFont);
			nextCharacter();
			setLock(false);
		}
	}
}