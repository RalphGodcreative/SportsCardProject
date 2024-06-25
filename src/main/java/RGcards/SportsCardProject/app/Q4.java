package RGcards.SportsCardProject.app;

public class Q4 {
    public static void main(String[] args) {
        int[][] schedule = new int[40][2];
        int[] workload = new int[8];
        boolean validSchedule=false;
        while (validSchedule==false){
            setWorkload(workload);
            schedule=new int[40][2];
            setSchedule(schedule,workload,99,99);
            validSchedule=valid(workload);
            System.out.println("again");
        }

        printSchedule(schedule);
    }

    public static void setSchedule(int[][] schedule, int[] workload , int mor1 ,int mor2){
        for (int i = 1; i < schedule.length; i += 2) {
            while (true) {
                int first = (int) (Math.random() * 8);
                int sec = (int) (Math.random() * 8);
                if (first != sec && workload[first] > 0 && workload[sec] > 0) {
                    mor1 = first;
                    mor2 = sec;
                    break;
                }
            }
            while (true) {
                int first = (int) (Math.random() * 8);
                int sec = (int) (Math.random() * 8);
                if(first != sec && workload[first] > 0 && workload[sec] > 0 &&
                        first!=mor1 && first!=mor2 && sec!=mor1 && sec!=mor2){
                    schedule[i-1][0]=mor1;
                    schedule[i-1][1]=mor2;
                    schedule[i][0]=first;
                    schedule[i][1]=sec;
                    workload[mor1]--;
                    workload[mor2]--;
                    workload[first]--;
                    workload[sec]--;
                    break;

                }
            }
        }
    }

    public static void setWorkload(int[] workload) {
        for (int i = 0; i < workload.length; i++) {
            if (i < 3) {
                workload[i] = 20;
            } else {
                workload[i] = 10;
            }
        }
    }

    public static void printSchedule(int[][] schedule){
        for(int i=1;i<schedule.length;i+=2){
            int day = (i / 2)+1;
            System.out.print("Day "+day+ " 早班 : "+getEmployee(schedule[i-1][0]) +" , "+getEmployee(schedule[i-1][1]));
            System.out.println(" 晚班 : "+getEmployee(schedule[i][0]) +" , "+getEmployee(schedule[i][1]) );
        }
    }

    public static String getEmployee(int x){
        switch (x){
            case 0:
                return "A";
            case 1:
                return "B";
            case 2:
                return "C";
            case 3:
                return "D";
            case 4:
                return "E";
            case 5:
                return "F";
            case 6:
                return "G";
            case 7:
                return "H";
            default:
                return "";
        }
    }

    public static boolean valid(int[] workload){
        for (int i=0;i<workload.length;i++){
            if(i<3){
                if(workload[i]>5){
                    System.out.println(i +" , "+workload[i]);
                    return false;
                }
            }else{
                if(workload[i]>8){
                    System.out.println(i +" , "+workload[i]);
                    return false;
                }
            }
        }
        System.out.println("good");
        return true;
    }
}
