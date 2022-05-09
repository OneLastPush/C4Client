package application;

import static java.lang.System.out;

import java.io.IOException;
import java.net.Socket;

import game.C4Game;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import networking.C4Msg;
import networking.C4Net;

/**
 * JavaFX controller associated with the JavaFX scene. Used to update the GUI.
 * This is the main logic for the client.
 * 
 * @author Julien Comtois, Frank Birikundavyi, Marjorie Olano Morales
 * @version 11/2/2015
 */
public class C4Controller {

	private Socket socket;
	private C4Net net;
	private C4Game game;
	private Circle[][] circles;
	private boolean isBusy;
	private boolean isGameOver;
	private int wins;
	private int losses;
	private int ties;
	private int tokensLeft;
	private byte[] packet;

	// Values injected by FXMLLoader
	@FXML
	private Button btnConnect;

	@FXML
	private Button btnYes;

	@FXML
	private Button btnNo;

	@FXML
	private Button btnNewGame;

	@FXML
	private GridPane gridPane;

	@FXML
	private Text textConnectInfo;

	@FXML
	private Text textGameInfo;

	@FXML
	private Text textIP;

	@FXML
	private Text textWins;

	@FXML
	private Text textLosses;

	@FXML
	private Text textTies;

	@FXML
	private Text textTokensLeft;

	@FXML
	private Text textPlayAgain;

	@FXML
	private TextField tfIP;

	/**
	 * Event listener for clicking the game board. Most packets received are
	 * handled here.
	 * 
	 * @param event
	 *            The event which called this method.
	 */
	@FXML
	void clickBoard(MouseEvent event) {
		if (!isGameOver && !isBusy) {
			textGameInfo.setText("");
			// Prevent click spam
			isBusy = true;
			int column;
			try {
				// Find which node was clicked
				String id = event.getPickResult().getIntersectedNode()
						.getId();
				// +3 for internal vs GUI
				column = Integer.parseInt(id) % 10 + 3;
			} catch (NullPointerException | NumberFormatException e) {
				// The user made an ambiguous click. (Not on a circle)
				// The user must click again on a circle to make the move.
				isBusy = false;
				return;
			}
			try {
				C4Msg msg;
				int row;
				// Check if move is valid before attempting to send to server
				if ((row = game.makeClientMove(column)) != -1) {
					net.sendPacket(C4Msg.CLIENT_MOVE, column);
					packet = net.receivePacket();
					msg = C4Msg.values()[packet[0]];
				} else {
					msg = C4Msg.BAD_MOVE;
				}
				// Evaluate received packet
				switch (msg) {
				case SERVER_MOVE:
					game.placeAIMove(packet[1]);
					// Display client token
					placeToken(false, row, column);
					tokensLeft--;
					textTokensLeft.setText("Tokens: " + tokensLeft);
					// Display AI token
					placeToken(true, packet[1] / 10, packet[1] % 10);
					break;
				case BAD_MOVE:
					textGameInfo.setText("Invalid move.");
					textGameInfo.setFill(Color.RED);
					break;
				case GAME_WON_CLIENT:
					placeToken(false, row, column);
					isGameOver = true;
					wins++;
					textWins.setText("Wins: " + wins);
					textGameInfo.setText("You win!");
					textGameInfo.setFill(Color.GREEN);
					showPlayAgainControls(true);
					break;
				case GAME_WON_AI:
					placeToken(false, row, column);
					placeToken(true, packet[1] / 10, packet[1] % 10);
					isGameOver = true;
					losses++;
					textLosses.setText("Losses: " + losses);
					textGameInfo.setText("You lose.");
					textGameInfo.setFill(Color.RED);
					showPlayAgainControls(true);
					break;
				case GAME_ENDED_TIE:
					placeToken(false, row, column);
					placeToken(true, packet[1] / 10, packet[1] % 10);
					isGameOver = true;
					ties++;
					textTies.setText("Ties: " + ties);
					textGameInfo.setText("Tie game.");
					textGameInfo.setFill(Color.BLUE);
					showPlayAgainControls(true);
					break;
				default:
					break;
				}
				isBusy = false;
			} catch (IOException e) {
				notifyOfCommIssue();
			}
		}
	}

	/**
	 * Event handler to handle the use clicking the connect button. Establishes
	 * the connection to the server and sets up the game as well as showing /
	 * hiding relevant / irrelevant UI elements.
	 * 
	 * @param event
	 *            The event which called this method.
	 */
	@FXML
	void connect(ActionEvent event) {
		String ip = tfIP.getText();
		// Leaving the IP field empty will use localhost as the IP
		if (!ip.equals("") && !validateIP(ip)) {
			textConnectInfo.setText("Invalid IP. Please try again.");
			return;
		}
		textConnectInfo.setText("");
		try {
			// Connect to server
			socket = new Socket(ip, C4Net.SERVER_PORT);
			net = new C4Net(socket);
			packet = net.receivePacket();
			if (packet[0] == C4Msg.START_GAME.ordinal()) {
				// Set up the game
				game = new C4Game();
				tfIP.setVisible(false);
				btnConnect.setVisible(false);
				textIP.setVisible(false);
				gridPane.setVisible(true);
				btnNewGame.setVisible(true);
				textWins.setVisible(true);
				textLosses.setVisible(true);
				textTies.setVisible(true);
				textTokensLeft.setVisible(true);
				textGameInfo.setText("Have fun!");
				textGameInfo.setFill(Color.BLUE);
				tokensLeft = 21;
				isGameOver = false;
				drawBoard();
			} else {
				out.println("Game could not be started.");
			}
		} catch (IOException e) {
			textConnectInfo.setText("Could not find server.");
		}
	}

	/**
	 * Let the server know we closed the game.
	 * 
	 * @throws IOException
	 *             Would not make sense to handle this since we want to program
	 *             to close anyways.
	 */
	void disconnect() throws IOException {
		if (net != null) {
			net.sendPacket(C4Msg.CLOSE_CONNECTION);
		}
	}

	/**
	 * Click event listener for the no button. Closes the client. Implicitly
	 * calls the stop method in the C4App class which in turn calls the
	 * disconnect method in order to sever the connection to the server.
	 * 
	 * @param event
	 *            The event which called this method.
	 */
	@FXML
	void exitGame(MouseEvent event) {
		// Close the client
		Platform.exit();
	}

	/**
	 * Click event listener for the yes button. Resets the game.
	 * 
	 * @param event
	 *            The event which called this method.
	 */
	@FXML
	void playAgain(MouseEvent event) {
		int msg = 0;
		try {
			net.sendPacket(C4Msg.PLAY_AGAIN);
			msg = net.receivePacket()[0];
		} catch (IOException e) {
			notifyOfCommIssue();
		}
		if (C4Msg.values()[msg] == C4Msg.START_GAME) {

			game = new C4Game();
			resetBoard();
			showPlayAgainControls(false);
			textGameInfo.setText("");
			tokensLeft = 21;
			textTokensLeft.setText("Tokens: " + tokensLeft);
			isGameOver = false;
		} else {
			out.println("Game could not be started.");
		}
	}

	/**
	 * Adds the required elements to the board in the GUI.
	 */
	private void drawBoard() {
		circles = new Circle[6][7];
		Circle circle;
		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 7; col++) {
				circle = new Circle(50);
				circle.setFill(Color.WHITE);
				circle.setId(row + "" + col);
				circles[row][col] = circle;
				gridPane.add(circle, col, row);
			}
		}
		gridPane.setStyle("-fx-background-color: #00A2E8;");
	}

	/**
	 * Informs the user a comm issue has occurred and brings them back to the
	 * main connect screen.
	 */
	private void notifyOfCommIssue() {
		isBusy = false;
		isGameOver = true;
		tfIP.setVisible(true);
		btnConnect.setVisible(true);
		textIP.setVisible(true);
		gridPane.setVisible(false);
		btnNewGame.setVisible(false);
		textWins.setVisible(false);
		textLosses.setVisible(false);
		textTies.setVisible(false);
		textTokensLeft.setVisible(false);
		textConnectInfo.setVisible(true);
		textConnectInfo.setText("Communication problem occured.");
	}

	/**
	 * Places the visible "tokens" on the GUI board.
	 * 
	 * @param isAi
	 *            boolean representing whether it is a server or client move.
	 * @param row
	 *            The row to place the token in.
	 * @param col
	 *            The column to place the token in.
	 */
	private void placeToken(boolean isAi, int row, int col) {
		// -3 for GUI vs internal
		Circle circle = circles[row - 3][col - 3];
		if (isAi) {
			// Change color to black if it's an AI's token
			circle.setFill(Color.BLACK);
		} else {
			// Change color to red if it's a player's token
			circle.setFill(Color.RED);
		}
	}

	/**
	 * Resets the board for a new game.
	 */
	private void resetBoard() {
		// Loop through the whole board and reset all tokens to invisible
		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 7; col++) {
				circles[row][col].setFill(Color.WHITE);
			}
		}
	}

	/**
	 * Shows or hides the buttons and text to ask the user whether they want to
	 * play again.
	 * 
	 * @param show
	 *            boolean, true for show, false for hide
	 */
	private void showPlayAgainControls(boolean show) {
		textPlayAgain.setVisible(show);
		btnYes.setVisible(show);
		btnNo.setVisible(show);
	}

	/**
	 * Validates the IP entered by the user.
	 * 
	 * @param ip
	 *            The IP address to validate
	 * @return true if it's a valid IP, false otherwise
	 */
	private boolean validateIP(String ip) {
		// Get the 4 sets of numbers in an IP
		String[] dottedQuad = ip.split("\\.");
		// Ensure there are actually 4 sets
		if (dottedQuad.length != 4) {
			return false;
		}
		int num;
		for (String segment : dottedQuad) {
			try {
				num = Integer.parseInt(segment);
				// Range of valid numbers for an IP
				if (num > 255 || num < 0) {
					return false;
				}
			} catch (NumberFormatException e) {
				// Happens if someone puts non-numeric characters in
				return false;
			}
		}
		return true;
	}
}
