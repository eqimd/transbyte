public class Sorts {
    public static int[] bubbleSort(int[] sortArr){
        for (int i = 0; i < sortArr.length - 1; i++) {
            for(int j = 0; j < sortArr.length - i - 1; j++) {
                if(sortArr[j + 1] < sortArr[j]) {
                    int swap = sortArr[j];
                    sortArr[j] = sortArr[j + 1];
                    sortArr[j + 1] = swap;
                }
            }
        }

        return sortArr;
    }

    public static byte[] bubbleSortBytes(byte[] sortArr){
        for (int i = 0; i < sortArr.length - 1; i++) {
            for(int j = 0; j < sortArr.length - i - 1; j++) {
                if(sortArr[j + 1] < sortArr[j]) {
                    byte swap = sortArr[j];
                    sortArr[j] = sortArr[j + 1];
                    sortArr[j + 1] = swap;
                }
            }
        }

        return sortArr;
    }

    public static int[] selectionSort(int[] sortArr) {
        for (int i = 0; i < sortArr.length; i++) {
            int pos = i;
            int min = sortArr[i];
            //цикл выбора наименьшего элемента
            for (int j = i + 1; j < sortArr.length; j++) {
                if (sortArr[j] < min) {
                    //pos - индекс наименьшего элемента
                    pos = j;
                    min = sortArr[j];
                }
            }
            sortArr[pos] = sortArr[i];
            //меняем местами наименьший с sortArr[i]
            sortArr[i] = min;
        }

        return sortArr;
    }

    public static byte[] selectionSortBytes(byte[] sortArr) {
        for (int i = 0; i < sortArr.length; i++) {
            int pos = i;
            byte min = sortArr[i];
            //цикл выбора наименьшего элемента
            for (int j = i + 1; j < sortArr.length; j++) {
                if (sortArr[j] < min) {
                    //pos - индекс наименьшего элемента
                    pos = j;
                    min = sortArr[j];
                }
            }
            sortArr[pos] = sortArr[i];
            //меняем местами наименьший с sortArr[i]
            sortArr[i] = min;
        }

        return sortArr;
    }

    public static int[] selectionSortLikeTransalg(int[] data) {
        int size = data.length;
        for (int i = 0; i < size - 1; i++) {
            int min = i;
            for (int j = i+1; j < size; j++) {
                for (int k = i; k < size; k++) {
                    if ((min == k) && (data[j] < data[k])) {
                        min = j;
                    }
                }
            }
            for (int k = i; k < size; k++) {
                if (min == k) {
                    int t = data[i];
                    data[i] = data[k];
                    data[k] = t;
                }
            }
        }

        return data;
    }

    public static int[] insertionSort(int[] sortArr) {
        int j;
        //сортировку начинаем со второго элемента, т.к. считается, что первый элемент уже отсортирован
        for (int i = 1; i < sortArr.length; i++) {
            //сохраняем ссылку на индекс предыдущего элемента
            int swap = sortArr[i];
            for (j = i; j > 0 && swap < sortArr[j - 1]; j--) {
                //элементы отсортированного сегмента перемещаем вперёд, если они больше элемента для вставки
                sortArr[j] = sortArr[j - 1];
            }
            sortArr[j] = swap;
        }

        return sortArr;
    }

    public static byte[] insertionSortBytes(byte[] sortArr) {
        int j;
        //сортировку начинаем со второго элемента, т.к. считается, что первый элемент уже отсортирован
        for (int i = 1; i < sortArr.length; i++) {
            //сохраняем ссылку на индекс предыдущего элемента
            byte swap = sortArr[i];
            for (j = i; j > 0 && swap < sortArr[j - 1]; j--) {
                //элементы отсортированного сегмента перемещаем вперёд, если они больше элемента для вставки
                sortArr[j] = sortArr[j - 1];
            }
            sortArr[j] = swap;
        }

        return sortArr;
    }

    public static int[] insertionSortLikeTransalg(int[] data) {
        int size = data.length;
        for (int i = 1; i < size; i++) {
            int item = data[i];
            boolean cond = true;
            int index = i;

            for (int j = i; j > 0; j--) {
                cond = cond & (data[j - 1] > item);
                if (cond) {
                    data[j] = data[j - 1];
                    index = j - 1;
                }
            }

            for (int k = 0; k < i; k++) {
                if (index == k) {
                    data[k] = item;
                }
            }
        }

        return data;
    }
}