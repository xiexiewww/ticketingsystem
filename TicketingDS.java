package ticketingsystem;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import ticketingsystem.LockFreeHashSet;

public class TicketingDS implements TicketingSystem {

	private int routenum;
	private int coachnum;
	private int seatnum;
	private int stationnum;
	@SuppressWarnings("unused")
	private int threadnum;
	private int totalSeats;
	private LockFreeList<Ticket> ticList = new LockFreeList<Ticket>();
	private Seat seatSet[] ;
	ReentrantLock lock[];
	AtomicInteger counter = new AtomicInteger(0);
	
	public TicketingDS(int routenum, int coachnum, 
			int seatnum, int stationnum, int threadnum) {
		// TODO Auto-generated constructor stub
		this.routenum = routenum;
		this.coachnum = coachnum;
		this.seatnum = seatnum;
		this.stationnum = stationnum;
		this.threadnum = threadnum;
		this.totalSeats = coachnum*seatnum;
		this.seatSet = new Seat[this.routenum+1]; 
		this.lock = new ReentrantLock[this.routenum+1];
		for(int i=0; i<this.routenum+1;i++) {
			seatSet[i] = new Seat(this.stationnum+1,this.totalSeats+1);
			lock[i] = new ReentrantLock(); 
		}
	}

	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		// TODO Auto-generated method stub
		Ticket ticket = new Ticket();
		ticket.passenger = passenger;
		ticket.route = route;
		ticket.departure = departure;
		ticket.arrival = arrival;
		lock[route].lock();
		try {
			int seatId=0;
			int flag=0;
				while(flag==0) {
					long t = System.currentTimeMillis();
					Random r1 = new Random(t);
					int i=Math.abs(r1.nextInt() % 800);
					for(int j=departure-1; j<arrival; j++) {
						if(seatSet[route].getSeat(j, i) == 1) {
							seatId++;				
						}
					}
					if(seatId==0 && flag==0) {
						ticket.coach = i/this.seatnum + 1;
						ticket.seat = i % this.seatnum+1;
						flag=1;
						for(int k=departure-1; k<arrival; k++) {
							seatSet[route].setSeatFull(k, i);
						}
					}
					seatId=0;
				}
				if(flag == 0) {
					return null;
				}
		}finally {
			lock[route].unlock();		
		}
		ticket.tid = counter.getAndIncrement();
		if(ticList.add(ticket))
			return ticket;
		return null;
	}

	@Override
	public int inquiry(int route, int departure, int arrival) {
		int ticketNum=0;
		int station;
		int seat;
		int flag=1;
		for(seat = 0; seat < this.totalSeats; seat++) {
			for(station = departure-1; station < arrival; station++) {
				if(seatSet[route].getSeat(station, seat) == 0) {
					flag=0;
				}
			}
			if(flag == 0)
				ticketNum++;
			flag=1;
		}
		return ticketNum;
	}

	@Override
	public boolean refundTicket(Ticket ticket) {
		// TODO Auto-generated method stub
		if(!ticList.remove(ticket)) {
			return false;
			
		}
		else {
			int coach = ticket.coach;
			int seat = ticket.seat;
			int route = ticket.route;
			int departure = ticket.departure;
			int arrival = ticket.arrival;
			lock[route].lock();
			try {
				int seatId = (coach-1)*this.seatnum+seat-1;
				for(int i=departure-1; i < arrival; i++) {
					seatSet[route].setSeatEmpty(i, seatId);
				}
			}finally {
				lock[route].unlock();
			}
			return true;
		}
	}
	//ToDo
	
	class Seat{
		private int seats[][];
		public Seat(int station, int totalSeats){
				seats = new int[station+1][totalSeats+1];
				for(int j=0; j<station;j++) {
					for(int k=0; k<totalSeats; k++)
						seats[station][totalSeats]=0;
				}
		}
		public int getSeat(int station, int totalSeats) {
			return seats[station][totalSeats];
		}
		public void setSeatFull(int station, int totalSeats) {
			seats[station][totalSeats]=1;
		}
		public void setSeatEmpty(int station, int totalSeats) {
			seats[station][totalSeats]=0;
		}
	}
}
