package RGcards.SportsCardProject.util;

public class DataProcessUtil {

    public static String upperCaseFirstLetter(String str){
        StringBuilder sb = new StringBuilder();
        String[] array = str.split(" ");
        for(int i=0;i<array.length;i++){
            String word = array[i];
            if(word.length()>1){
                sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }else {
                sb.append(word);
            }
            if(i< array.length-1){
                sb.append(" ");
            }
        }

        return sb.toString();
    }
}
