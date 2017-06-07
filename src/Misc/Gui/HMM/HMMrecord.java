package Misc.Gui.HMM;

    public final class HMMrecord{
    	// instance varables
    	private String author;
    	private String song;
    	private String portFolio;
    	private int trackUsage;
    	
    	//constructor
    	public HMMrecord(String authorName, String HMMname, String portFolio, int trackLeng){
    	
    		setAuthor(authorName);
    		setHMM(HMMname);
    		setPortFolio(portFolio);
    		setHMMUsage(trackLeng);
    	}
    	
    	//set author name
    	public void setAuthor(String authorName){
    		author = authorName;
    	}
    	
    	//get author name
    	public String getAuthor(){
    		return author;
    	}
    	
    	//set song name
    	public void setHMM(String HMMname){
    		song = HMMname;
    	}
    	//get song name
    	public String getHMM(){
    		return song;
    	}
    	
    	//set portFolio name
    	public void setPortFolio(String prtFolio){
    		portFolio = prtFolio;
    	}
    	
    	//get ablum name
    	public String getPortFolio(){
    		return portFolio;
    	}
    	
    	// set track length
    	public void setHMMUsage(int trackLeng){
    		trackUsage = trackLeng;
    	}
    	
    	// get track length
    	public int getHMMUsage(){
    		return trackUsage;  
    	}
    	
    	// to string method
        @Override
    	public String toString(){
    		return String.format("%s, %s, %s, %d : %d", getAuthor(), 
    			getHMM(), getPortFolio(), getHMMUsage() / 60,  
    			getHMMUsage() - (getHMMUsage() / 60) * 60);
    	}
    }
