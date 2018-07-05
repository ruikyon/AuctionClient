package client;

import java.util.TimerTask;

public class MyTimer extends TimerTask {
	AuctionClient client;

	public MyTimer(AuctionClient ac) {
		client = ac;
	}

	public void run() {
		System.out.println("MyTimer: " + client.bid + ", " + client.State_No);
		if (client.bid) {
			client.bid = false;
		} else {
			client.finish();
		}
	}
}
