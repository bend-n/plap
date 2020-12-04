package main;

public class RTInterval {

    private int seconds;
    private int quotient = 0;
    public RTInterval(int seconds){
        this.seconds = seconds;
    }

    public boolean get(int time){
        if(time/seconds > quotient){
            quotient ++;
            return true;
        }
        return false;
    }

}
