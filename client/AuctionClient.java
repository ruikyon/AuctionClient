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
import java.net.SocketTimeoutException;
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

	private static final String HOST = "192.168.56.1";// "10.24.89.120",
														// 接続先サーバのホスト名(IPアドレス)
	private static final int PORT = 8080; // 接続先ポート番号

	private Socket socket; // このアプリケーションのクライアントソケット
	private Thread thread; // メッセージ受信監視用スレッド
	private Timer t;
	private int fileSize;

	public int State_No; // 状態番号
	public int exhibitNumber = 0; // 出品数のカウント(4品以上の出品は認めない)
	public boolean bid; // 入札が一定期間内にあったかを保持、タイマーで用いる
	public boolean imageIn; // 画像受信フラグ

	// 以下、コンポーネント
	private JList<String> exhibitList; // 出品物のリスト
	private JList<String> userList; // 現在入室中のユーザー
	private JLabel Item; // 現在オークション中の商品
	private JLabel Amount; // 現在の入札価格
	private JLabel image; // 画像
	private JTextArea msgTextArea; // メッセージを表示するテキストエリア
	private JTextField msgTextField; // 入札金額入力用の一行テキスト
	private JTextField bidTextField; // 入札金額入力用の一行テキスト
	private JButton submitButton; // 「送信」ボタン
	private JButton exhibitButton; // 出品ボタン
	private JButton ExitButton; // 退室ボタン
	private JButton msgButton; // メッセージ送信ボタン

	// コンストラクタ
	public AuctionClient(String name) throws IOException {
		frame.getContentPane().removeAll();// エントランスのコンポーネントをクリア

		JPanel topPanel = new JPanel();
		JPanel leftPanel = new JPanel();
		JPanel bottomPanel = new JPanel();
		JPanel centerPanel = new JPanel();
		JPanel exhibitPanel = new JPanel();
		JPanel userPanel = new JPanel();
		JPanel myPanel = new JPanel();
		JPanel infoPanel = new JPanel();

		exhibitList = new JList<String>();
		userList = new JList<String>();
		Item = new JLabel("Wait..  ");
		Amount = new JLabel("\null");
		msgTextArea = new JTextArea();
		msgTextField = new JTextField();
		bidTextField = new JTextField();

		msgButton = new JButton("送信");
		msgButton.addActionListener(this);
		msgButton.setActionCommand("message");
		submitButton = new JButton("入札");
		submitButton.addActionListener(this);
		submitButton.setActionCommand("submit");
		exhibitButton = new JButton("出品");
		exhibitButton.addActionListener(this);
		exhibitButton.setActionCommand("exhibit");
		ExitButton = new JButton("退室する");
		ExitButton.addActionListener(this);
		ExitButton.setActionCommand("exit");

		image = new JLabel("");
		setImage("default.jpg");

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
		} else {
			frame.setVisible(true);
		}
	}

	// メッセージ監視用スレッド
	public void run() {
		try {
			socket.setSoTimeout(0);
			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			while (!socket.isClosed()) {
				if (!imageIn) {
					String line = reader.readLine();
					System.out.println("Input: " + line);

					String[] msg = line.split(" ", 2);
					String msgName = msg[0];
					String msgValue = (msg.length < 2 ? "" : msg[1]);
					reachedMessage(msgName, msgValue);
				} else {
					System.out.println("image receiving: " + fileSize);
					String outFile = "out.jpg"; // 受信したファイルの保存先
					byte[] buffer = new byte[512]; // ファイル受信時のバッファ
					OutputStream output = new FileOutputStream(outFile);// 送信ストリーム

					// ファイルをストリームで受信
					int fileLength, totalLength = 0;
					socket.setSoTimeout(5000);
					try {
						while ((fileLength = input.read(buffer)) > 0) {
							output.write(buffer, 0, fileLength);
							totalLength += fileLength;
							if (totalLength >= fileSize)
								break;
						}
					} catch (SocketTimeoutException ste) {
						outFile = "default.jpg";
						System.out.println("time out");
						sendMessage("echo");
					} finally {
						socket.setSoTimeout(0);
					}

					// 終了処理
					System.out.println("finish :" + totalLength);
					output.flush();
					output.close();
					imageIn = false;
					setImage(outFile);
				}
			}
		} catch (Exception err) {// サーバが強制終了した、あるいはサーバが開いていない
			errorState();
			err.printStackTrace();
			msgTextArea.append(">オークションが終了しました。ウィンドウを閉じてください\n");
		}
	}

	// サーバーに接続する
	private boolean connectServer() {
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
	private void close() throws IOException {
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
			msgTextArea.append("SYSTEM>メッセージの送信に失敗しました\n");
		}
	}

	// サーバーから送られてきたメッセージの処理
	private void reachedMessage(String name, String value) throws IOException {
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

		case "change":// 現在の商品が変更された
			String[] temp1 = value.split(",", 3);
			Item.setText(temp1[0]);
			Amount.setText("￥" + temp1[1]);
			fileSize = Integer.parseInt(temp1[2]);
			imageIn = true;
			questionTime();
			break;

		case "seller":// 売り手になった
			String[] temp2 = value.split(",", 3);
			Item.setText(temp2[0]);
			Amount.setText("￥" + temp2[1]);
			sendImage(temp2[2]);
			setImage(temp2[2]);
			t = new Timer();
			t.schedule(new MyTimer(this), 60000, 60000);
			questionTime();
			submitButton.setEnabled(true);
			submitButton.setText("終了");
			submitButton.setActionCommand("msgfin");
			break;

		case "wait":// 自身が出品した商品のオークションがスタートした
			becomeSeller();
			msgTextArea.append("SYSTEM>入札開始\n");
			break;

		case "go":// 入札が開始した
			becomeBuyer();
			msgTextArea.append("SYSTEM>入札開始\n");
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
				sendMessage("getImage");
				break;
			case 2:
				becomeBuyer();
				sendMessage("getImage");
				break;
			}
			break;

		case "error":// エラーが発生した
			msgTextArea.append(value + "\n");
			break;

		case "Stop":// 同じ名前のユーザがすでに参加していた
			msgTextArea.append("ERROR>同じ名前のユーザがいたためオークションに参加できませんでした\n");
			msgTextArea.append("ERROR>サーバから切断されました。ウィンドウを閉じてください\n");
			errorState();
			socket.close();
			break;

		case "image":
			imageIn = true;
			fileSize = Integer.parseInt(value);
			break;

		case "echo":
			sendMessage("res");
			break;

		default:
			break;
		}
	}

	// 文字列が自然数であるかどうか
	private boolean isNumber(String num) {
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
		case "submit":// ベッティング
			String betAmount = bidTextField.getText();

			// 不正な入力(文字列,少数等)の防止
			if (isNumber(betAmount)) {
				sendMessage("bet " + Integer.parseInt(betAmount));
				bidTextField.setText("");
			} else {
				msgTextArea.append("ERROR>適切な数値を入力してください\n");
			}
			break;

		case "exhibit":// 出品
			new RequestExhibit(this);
			exhibitButton.setEnabled(false);
			break;

		case "decide":// 落札
			finish();
			break;

		case "exit":// 退室
			try {
				close();
				ExitButton.setEnabled(false);
			} catch (Exception err) {
			}
			break;

		case "message":// メッセージ送信
			sendMessage("msg " + msgTextField.getText());
			msgTextField.setText("");
			break;

		case "msgfin":// 質疑応答終了
			finish();
			break;

		default:
			break;
		}
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
		exhibitButton.setEnabled(true);
		bidTextField.setEnabled(false);
		submitButton.setEnabled(true);
		submitButton.setText("落札");
		submitButton.setActionCommand("decide");

		bid = false;
		State_No = 2;
		t = new Timer();
		t.schedule(new MyTimer(this), 20000, 20000);
	}

	// 入札している状態のコンポーネント設定(State_No=3)
	private void becomeBuyer() {
		// 出品は可能, ベッティングも可能
		exhibitButton.setEnabled(true);
		bidTextField.setEnabled(true);
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
		submitButton.setEnabled(false);
		submitButton.setText("待ち");
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
		sendMessage("image " + file.length());
		byte[] buffer = new byte[256]; // ファイル送信時のバッファ
		System.out.println("image sending");

		try {
			// ストリームの準備
			InputStream inputStream = new FileInputStream(file);
			OutputStream outputStream = socket.getOutputStream();

			// ファイルをストリームで送信
			int fileLength;
			while ((fileLength = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, fileLength);
			}

			// 終了処理
			outputStream.flush();
			inputStream.close();
			System.out.println("finish");
		} catch (IOException ioe) {
			msgTextArea.append("ERROR>画像送信に失敗しました\n");
			ioe.printStackTrace();
		}
	}

	public void exhClose() {
		exhibitButton.setEnabled(true);
	}

	public void finish() {
		t.cancel();
		if (State_No == 2)
			sendMessage("Decide");
		else if (State_No == 4)
			sendMessage("Start");
	}
}