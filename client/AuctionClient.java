package client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class AuctionClient implements Runnable, ActionListener {

	private static final JFrame frame = new JFrame("オークション");

	public static void main(String[] args) throws IOException {

		new Entrance(frame); // エントランスを開く

		frame.setSize(800, 600);
		frame.setVisible(true);
	}

	private static final String HOST = "localhost";// "10.24.89.120";//
													// 接続先サーバのホスト名(IPアドレス)

	private static final int PORT = 8080; // 接続先ポート番号

	private Socket socket; // このアプリケーションのクライアントソケット

	private Thread thread; // メッセージ受信監視用スレッド

	public int State_No; // 状態番号

	public int exhibitNumber = 0; // 出品数のカウント(4品以上の出品は認めない)

	private Timer t;
	//private String userName;

	// 以下、コンポーネント
	private JList<String> exhibitList; // 出品物のリスト
	private JList<String> userList; // 現在入室中のユーザー
	private JLabel Item; // 現在オークション中の商品
	private JLabel Amount; // 現在の入札価格
	private JLabel image;
	private JTextArea msgTextArea; // メッセージを表示するテキストエリア
	private JTextField msgTextField; // 入札金額入力用の一行テキスト
	private JTextField bidTextField; // 入札金額入力用の一行テキスト
	private JButton submitButton; // 「送信」ボタン
	private JButton exhibitButton; // 出品ボタン
	private JButton ExitButton; // 退室ボタン
	private JButton msgButton;

	public boolean bid;
	public boolean reConnect;
	public boolean imageIn;
	// private File out, in;

	// コンストラクタ
	public AuctionClient(String name) throws IOException {
		//userName = name;

		frame.getContentPane().removeAll();// エントランスのコンポーネントをクリアにしておく

		JPanel topPanel = new JPanel();
		JPanel leftPanel = new JPanel();
		JPanel bottomPanel = new JPanel();
		JPanel centerPanel = new JPanel();

		JPanel exhibitPanel = new JPanel();
		JPanel userPanel = new JPanel();

		JPanel myPanel = new JPanel();
		JPanel infoPanel = new JPanel();

		// JPanel imagePanel = new JPanel();

		exhibitList = new JList<String>();
		userList = new JList<String>();

		Item = new JLabel("Wait..  ");
		Amount = new JLabel("\null");
		// MaxBidder = new JLabel("-");

		msgTextArea = new JTextArea();
		msgTextField = new JTextField();
		bidTextField = new JTextField();
		msgButton = new JButton("送信");
		submitButton = new JButton("入札");
		exhibitButton = new JButton("出品");

		msgButton.addActionListener(this);
		msgButton.setActionCommand("message");

		submitButton.addActionListener(this);
		submitButton.setActionCommand("submit");

		exhibitButton.addActionListener(this);
		exhibitButton.setActionCommand("exhibit");

		ExitButton = new JButton("退室する");

		ExitButton.addActionListener(this);
		ExitButton.setActionCommand("exit");

		exhibitPanel.setLayout(new BorderLayout());
		exhibitPanel.add(new JLabel("出品リスト"), BorderLayout.NORTH);
		exhibitPanel.add(new JScrollPane(exhibitList), BorderLayout.CENTER);

		userPanel.setLayout(new BorderLayout());
		userPanel.add(new JLabel("参加ユーザー"), BorderLayout.NORTH);
		userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

		leftPanel.setLayout(new GridLayout(2, 1));
		leftPanel.add(exhibitPanel);
		leftPanel.add(userPanel);

		topPanel.setLayout(new GridLayout(1, 2));
		topPanel.add(myPanel);
		topPanel.add(infoPanel);

		myPanel.setLayout(new FlowLayout());
		myPanel.add(new JLabel("ユーザ名:" + name + "                  "));
		myPanel.add(ExitButton);

		infoPanel.setLayout(new FlowLayout());
		infoPanel.add(exhibitButton);
		infoPanel.add(new JLabel("現在商品名:"));
		infoPanel.add(Item);
		infoPanel.add(new JLabel("            現在価格:"));
		infoPanel.add(Amount);

		image = new JLabel("");
		// image.setSize(500, 500);
		setImage("default.jpg");
		// imagePanel.add(image);

		bottomPanel.setLayout(new FlowLayout());
		bottomPanel.add(new JLabel("         金額入力:"));
		bottomPanel.add(bidTextField);
		bottomPanel.add(submitButton);
		bottomPanel.add(new JLabel("   メッセージ入力:"));
		bottomPanel.add(msgTextField);
		bottomPanel.add(msgButton);

		msgTextField.setPreferredSize(new Dimension(200, msgTextField.getPreferredSize().height));
		bidTextField.setPreferredSize(new Dimension(200, bidTextField.getPreferredSize().height));

		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(new JScrollPane(msgTextArea), BorderLayout.CENTER);
		centerPanel.add(new JLabel("メッセージ"), BorderLayout.NORTH);

		frame.getContentPane().add(centerPanel, BorderLayout.CENTER);
		frame.getContentPane().add(leftPanel, BorderLayout.WEST);
		frame.getContentPane().add(image, BorderLayout.EAST);
		frame.getContentPane().add(topPanel, BorderLayout.NORTH);
		frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		msgTextArea.setEditable(false);// テキストエリアはメッセージを表示するだけなので編集不可に設定

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ウィンドウのクローズボタンの仕様定義
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					close();
				} catch (Exception err) {
				}
			}
		});

		if (connectServer()) { // サーバに接続
			// メッセージ受信監視用のスレッドを生成してスタートさせる
			thread = new Thread(this);
			thread.start();

			// 名前の登録をサーバに申請する
			sendMessage("setName " + name);

			// 現在の商品、出品状況をサーバと同期する
			sendMessage("getExhibits");
			sendMessage("getCurrentExhibit");
			sendMessage("getAmount");
			sendMessage("getState");
			// run();
		} else {
			frame.setVisible(true);
		}
	}

	// サーバーに接続する
	public boolean connectServer() {
		try {
			socket = new Socket(HOST, PORT);
			msgTextArea.append(">サーバーに接続しました\n");
			return true;
		}
		// サーバが開いていない
		catch (Exception err) {
			errorState();
			msgTextArea.append("ERROR>サーバの接続に失敗しました\n");
			return false;
		}
	}

	// サーバーから切断する
	public void close() throws IOException {
		// 出品者の場合
		/*
		 * if (State_No == 1 || State_No == 2) { sendMessage("Break"); }
		 */
		sendMessage("close");
		socket.close();
	}

	// メッセージをサーバーに送信する
	public void sendMessage(String msg) {
		try {
			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output);

			writer.println(msg);
			writer.flush();
		} catch (Exception err) {
		}
	}

	// サーバーから送られてきたメッセージの処理
	public void reachedMessage(String name, String value) throws IOException {
		switch (name) {
		case "exhibits":// 出品物のリストに変更が加えられた
			if (value.equals("")) {
				exhibitList.setModel(new DefaultListModel<String>());
			} else {
				String[] exhibits = value.split(" ");
				exhibitList.setListData(exhibits);
			}
			break;

		case "currentExhibit":// 現在の商品を表示(途中入場用)
			Item.setText(value);
			break;

		case "amount":// 現在の入札価格を表示(途中入場用)
			Amount.setText("￥" + value);
			break;

		case "users":// ユーザーが入退室した
			if (value.equals("")) {
				userList.setModel(new DefaultListModel<String>());
			} else {
				String[] users = value.split(" ");
				userList.setListData(users);
			}
			break;

		case "msg":// メッセージが送られてきた
			msgTextArea.append(value + "\n");
			break;

		case "change":// 現在の商品が変更された Item.setText(value);
			// Amount.setText("￥null");
			String[] temp1 = value.split(",", 3);
			Item.setText(temp1[0]);
			Amount.setText("￥" + temp1[1]);
			imageIn = true;
			//setImage(temp1[2]);

			questionTime();
			break;

		case "seller":// 売り手になった Item.setText(value);
			sendMessage("image");
			String[] temp2 = value.split(",", 3);
			Item.setText(temp2[0]);
			Amount.setText("￥" + temp2[1]);
			sendImage(temp2[2]);
			setImage(temp2[2]);

			t = new Timer();
			t.schedule(new MyTimer(this), 60000, 60000);
			questionTime();
			submitButton.setText("終了");
			submitButton.setActionCommand("msgfin");

			// msgTextArea.append("\n");
			break;

		case "wait":// 自身が出品した商品のオークションがスタートした
			becomeSeller();
			msgTextArea.append("オークション開始\n");
			break;

		case "go":// 入札が開始した
			becomeBuyer();
			msgTextArea.append("オークション開始\n");
			break;

		case "ok":// 出品リストに待ちがなくなった
			Item.setText("Wait.. ");
			Amount.setText("￥null");
			startState();
			break;

		case "changeValue":// 入札価格が変更された
			Amount.setText("￥" + value);
			bid = true;
			break;

		case "state":// 初期状態を決定する(途中入場用)
			switch (Integer.parseInt(value)) {
			case 0:
				startState();
				break;
			case 1:
				questionTime();
				break;
			case 2:
				becomeBuyer();
				break;
			}
			break;

		case "error":// エラーが発生した
			msgTextArea.append("ERROR>" + value + "\n");
			break;

		case "Stop":// 同じ名前のユーザがすでに参加していた
			msgTextArea.append("ERROR>同じ名前のユーザがいたためオークションに参加できませんでした\n");
			msgTextArea.append("ERROR>サーバから切断されました。ウィンドウを閉じてください\n");
			errorState();
			socket.close();
			break;

		case "reConnected":
			msgTextArea.append("ERROR>再接続できました\n");
			break;

		default:
			break;
		}
	}

	// メッセージ監視用スレッド
	public void run() {
		try {
			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			while (!socket.isClosed()) {
				System.out.println(imageIn);
				// try {
				if (!imageIn) {
					String line = reader.readLine();
					System.out.println("Input: " + line);

					if (line == null) {
						// System.out.println("reader: " + reader);
						continue;
					}

					String[] msg = line.split(" ", 2);
					String msgName = msg[0];
					String msgValue = (msg.length < 2 ? "" : msg[1]);
					reachedMessage(msgName, msgValue);
				} else {
					msgTextArea.append("SYSTEM>画像受信中…\n");
					System.out.println("image in prepare");
					String outFile = "out.jpg"; // 受信したファイルの保存先
					byte[] buffer = new byte[512]; // ファイル受信時のバッファ
					// ストリームの準備

					// InputStream inputstream = socket.getInputStream();
					OutputStream outputstream = new FileOutputStream(outFile);

					System.out.println("image in start");
					// ファイルをストリームで受信
					int fileLength;
					while ((fileLength = input.read(buffer)) > 0) {
						outputstream.write(buffer, 0, fileLength);
						System.out.println(fileLength + ":receiving...");
						if (fileLength != 512)
							break;
					}

					// 終了処理
					outputstream.flush();
					outputstream.close();
					imageIn = false;
					input = socket.getInputStream();
					reader = new BufferedReader(new InputStreamReader(input));
					// sendMessage("imageFin");
					msgTextArea.append("SYSTEM>画像受信完了\n");
					setImage(outFile);
					questionTime();
				}
				/*
				 * } catch (SocketException se) {
				 * msgTextArea.append("ERROR>サーバの接続が切れました\n");
				 * msgTextArea.append("ERROR>サーバへ再接続中...\n");
				 *
				 * while (true) { reConnect = false; new ReConnect(this); while
				 * (!reConnect) { } if (connectServer()) { input =
				 * socket.getInputStream(); reader = new BufferedReader(new
				 * InputStreamReader(input)); sendMessage("reConnect " +
				 * userName); break; } }
				 *
				 * }
				 */
			}
		}
		// サーバが強制終了した、あるいはサーバが開いていない
		catch (Exception err) {
			// System.out.println("socket: "+socket.isClosed());;
			errorState();
			err.printStackTrace();
			msgTextArea.append(">オークションが終了しました。ウィンドウを閉じてください\n");
		}
	}

	// 文字列が自然数であるかどうか
	public boolean isNumber(String num) {
		try {
			int i;
			i = Integer.parseInt(num);
			if (i <= 0)
				return false;
			else
				return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// ボタンが押されたときのイベント処理
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		switch (cmd) {
		// ベッティング
		case "submit":
			String betAmount = bidTextField.getText();

			// 不正な入力(文字列,少数等)の防止
			if (isNumber(betAmount)) {
				sendMessage("bet " + Integer.parseInt(betAmount));
				bidTextField.setText("");
			} else {
				msgTextArea.append("ERROR>適切な数値を入力してください\n");
			}
			break;

		// 出品
		case "exhibit":
			new RequestExhibit(this);
			exhibitButton.setEnabled(false);
			break;

		// スタート価格の決定
		case "start":
			String startPrice = bidTextField.getText();

			// 不正な入力の防止
			if (isNumber(startPrice)) {
				sendMessage("Start " + startPrice);
				bidTextField.setText("");
				exhibitNumber--; // 出品数を1つ減らす
			} else {
				msgTextArea.append("ERROR>適切な数値を入力してください\n");
			}
			break;

		// 落札
		case "decide":
			finish();
			break;

		// 退室
		case "exit":
			try {
				close();
				ExitButton.setEnabled(false);
			} catch (Exception err) {
			}
			break;

		case "message":
			sendMessage("msg " + msgTextField.getText());
			msgTextField.setText("");
			break;

		case "msgfin":
			finish();
			break;

		default:
			break;
		}
	}

	public void exhClose() {
		exhibitButton.setEnabled(true);
	}

	private void setImage(String fileName) {
		ImageIcon preIcon = new ImageIcon(fileName);

		Image smallImg = preIcon.getImage().getScaledInstance(350, -1, Image.SCALE_SMOOTH);

		ImageIcon icon = new ImageIcon(smallImg);

		image.setIcon(icon);

	}

	// 初期状態(State_No=0)
	private void startState() {

		msgTextArea.append("商品の出品待ちです\n");

		// 出品は可能, ベット操作は禁止
		exhibitButton.setEnabled(true);
		submitButton.setEnabled(false);
		submitButton.setText("待ち");
		msgButton.setEnabled(false);

		State_No = 0;
	}

	// 自身が出品した商品の入札が行われている状態のコンポーネント設定(State_No=2)
	private void becomeSeller() {

		// 出品は禁止, 落札のボタンのみ操作可能
		exhibitButton.setEnabled(false);
		bidTextField.setEnabled(false);
		submitButton.setEnabled(true);
		submitButton.setText("落札");
		submitButton.setActionCommand("decide");

		bid = false;
		State_No = 2;
		t = new Timer();
		t.schedule(new MyTimer(this), 20000, 20000);
	}

	// 初め値を入力中の状態(State_No=2)
	/*
	 * private void setStartPrice() {
	 *
	 * // 出品は禁止, スタート価格の入力が可能 exhibitButton.setEnabled(false);
	 * bidTextField.setEnabled(true); submitButton.setEnabled(true);
	 * submitButton.setText("スタート価格"); submitButton.setActionCommand("start");
	 *
	 * State_No = 2; }
	 */

	// 入札している状態のコンポーネント設定(State_No=3)
	private void becomeBuyer() {

		msgTextArea.append("入札開始\n");

		// 出品は可能, ベッティングも可能
		exhibitButton.setEnabled(true);
		submitButton.setEnabled(true);
		submitButton.setText("入札");
		submitButton.setActionCommand("submit");

		State_No = 3;
	}

	// スタート価格の入力を待つ状態のコンポーネント設定(State_No=4)
	private void questionTime() {

		msgTextArea.append("SYSTEM>質疑応答開始\n");

		// 出品は可能, ベッティングは不可
		exhibitButton.setEnabled(true);
		// bidTextField.setEnabled(true);
		submitButton.setEnabled(true);
		// submitButton.setText("待ち");
		msgButton.setEnabled(true);

		State_No = 4;
	}

	// サーバと通信できなくなった状態
	private void errorState() {
		// すべてのボタン操作禁止
		exhibitButton.setEnabled(false);
		bidTextField.setEnabled(false);
		submitButton.setEnabled(false);
		ExitButton.setEnabled(false);
	}

	private void sendImage(String fileName) {
		File file = new File(fileName); // 送信するファイルのオブジェクト
		byte[] buffer = new byte[512]; // ファイル送信時のバッファ

		try {
			// ストリームの準備
			InputStream inputStream = new FileInputStream(file);
			OutputStream outputStream = socket.getOutputStream();

			// ファイルをストリームで送信
			int fileLength;
			while ((fileLength = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, fileLength);
			}

			/*
			 * try{ Thread.sleep(5000); }catch(InterruptedException ie){
			 * ie.printStackTrace(); }
			 */
			// 終了処理
			outputStream.flush();
			inputStream.close();
			System.out.println("finish sending");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void finish() {
		t.cancel();
		if (State_No == 2)
			sendMessage("Decide");
		else if (State_No == 4)
			sendMessage("Start");
	}
}