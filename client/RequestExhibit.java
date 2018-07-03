package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

//出品時の画面
public class RequestExhibit implements ActionListener {

	// 以下コンポーネント
	JFrame frame;
	JLabel introduction = new JLabel("出品画面");
	JLabel user = new JLabel("商品の名前と初期価格を入力してください");
	JLabel nameT = new JLabel("商品の名前:");
	JLabel priceT = new JLabel("初期価格:  ");
	JLabel fileT = new JLabel("ファイル名:");//相対パスの場合はauctionフォルダに入れる

	JTextField name = new JTextField();
	JTextField price = new JTextField();
	JTextField file = new JTextField();
	JLabel err = new JLabel("");
	JButton submit = new JButton("申請");

	AuctionClient client;

	// コンストラクタ
	public RequestExhibit(AuctionClient ac) {
		client = ac;
		frame = new JFrame();
		frame.setSize(600, 450);
		frame.setVisible(true);

		introduction.setBounds(50, 30, 100, 30);

		user.setBounds(70, 170, 250, 30);

		name.setBounds(150, 200, 200, 30);
		nameT.setBounds(70, 200, 150, 30);
		price.setBounds(150, 230, 200, 30);
		priceT.setBounds(70, 230, 150, 30);
		file.setBounds(150, 260, 200, 30);
		fileT.setBounds(70, 260, 150, 30);

		submit.setBounds(100, 290, 100, 30);
		submit.addActionListener(this);

		err.setBounds(100, 1000, 100, 30);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				client.exhClose();
			}
		});

		// 以下コンポーネントをフレームに配置
		frame.getContentPane().add(introduction);
		frame.getContentPane().add(user);
		frame.getContentPane().add(name);
		frame.getContentPane().add(nameT);
		frame.getContentPane().add(price);
		frame.getContentPane().add(priceT);
		frame.getContentPane().add(file);
		frame.getContentPane().add(fileT);
		frame.getContentPane().add(submit);
		frame.getContentPane().add(err);

		String dir = System.getProperty("user.dir");
	    System.out.println("Projectのトップレベルのパス： " + dir);
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

	// ボタン操作(名前の入力)
	public void actionPerformed(ActionEvent e) {

		String Name = name.getText();
		String Price = price.getText();

		File f = new File(file.getText());

		int length = Name.getBytes().length;

		// 3文字未満の入力は禁止(再入力要請)
		if(!f.exists()){
			err.setText("ファイル名が正しくありません");
		}
		else if (length < 3) {
			err.setText("短すぎます(名前は半角3文字以上です)");
		}
		// 13文字以上の入力も禁止(再入力要請)
		else if (length > 12) {
			err.setText("長すぎます(名前は半角12文字以下です)");
		}
		// 名前に半角空白文字は使えない(再入力要請)
		else if (Name.indexOf(" ") != -1 || Name.indexOf("　") != -1) {
			err.setText("名前に空白文字を含むことはできません");
		}
		// 名前にハイフンは使えない(再入力要請)
		else if (Name.indexOf(",") != -1) {
			err.setText("名前にカンマを含むことはできません");
		} else if (!isNumber(Price)) {
			err.setText("適切な数値を入力してください");
		} else {
			client.sendMessage("addExhibit " + Name + "," + Integer.parseInt(Price) + "," + file.getText());
			frame.dispose();
		}
	}
}
