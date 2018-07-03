package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

//エントランスオブジェクト
public class Entrance implements ActionListener {

	// 以下コンポーネント
	JLabel introduction = new JLabel("ようこそ");
	JLabel user = new JLabel("名前を入力してください");
	JTextField name = new JTextField();
	JLabel err = new JLabel("");
	JButton submit = new JButton("入室する");

	// コンストラクタ
	public Entrance(JFrame frame) {

		introduction.setBounds(50, 30, 100, 30);

		user.setBounds(70, 200, 200, 30);

		name.setBounds(70, 230, 200, 30);

		submit.setBounds(300, 230, 100, 30);
		submit.addActionListener(this);

		err.setBounds(100, 1000, 100, 30);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ウィンドウのクローズボタンの仕様定義

		// 以下コンポーネントをフレームに配置
		frame.getContentPane().add(introduction);
		frame.getContentPane().add(user);
		frame.getContentPane().add(name);
		frame.getContentPane().add(submit);
		frame.getContentPane().add(err);
	}

	// ボタン操作(名前の入力)
	public void actionPerformed(ActionEvent e) {

		String Name = name.getText();

		int length = Name.getBytes().length;

		// 3文字未満の入力は禁止(再入力要請)
		if (length < 3) {
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
		else if (Name.indexOf("-") != -1) {
			err.setText("名前にハイフンを含むことはできません");
		} else {
			try {
				new AuctionClient(Name); // オークションに入る
			} catch (Exception error) {
			}
		}
	}
}
