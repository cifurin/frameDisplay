package com.example.framedisplay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpRequest.BodyPublishers;
//import java.net.http.HttpResponse;
//import java.net.http.HttpResponse.BodyHandlers;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String  TAG = "MAIN";

    static boolean Process = true;
    //static String sol = "";
    StringBuilder sol = new StringBuilder();

    static List<Rect> squares = new ArrayList<Rect>();
    static int[][] sym_row_dist = {{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}};
    static int[][] sym_col_dist = {{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}};
    static int n_dist[][] = {{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0}};

    static ArrayList<ArrayList<Double>> sym_row_norm = new ArrayList<ArrayList<Double>>();
    static ArrayList<ArrayList<Double>> sym_col_norm = new ArrayList<ArrayList<Double>>();
    static ArrayList<ArrayList<Double>> num_norm = new ArrayList<ArrayList<Double>>();

    static List<Integer> weighted = new ArrayList<Integer>();
    static List<Integer> num_weighted = new ArrayList<Integer>();

    Scalar red = new Scalar(255, 0, 0);
    Scalar green = new Scalar(0, 255, 0);
    Scalar blue = new Scalar(0, 0, 255);

    int fontface = Core.FONT_HERSHEY_SIMPLEX;

    int cx = 480/2;
    int cy = 640/2;
    int width = 40;
    int height = 40;

    int dyn_xmin = cx - 5 * width;
    int dyn_xmax = cx + 5 * width;
    int dyn_ymin = cy - 5 * height;
    int dyn_ymax = cy + 5 * height;

    //Mat gray, thresh, hierarchy;

    //Mat gray = new Mat(width, width, CvType.CV_8UC4);

    //MatOfPoint2f approxCurve;

    List<Point> points= new ArrayList<Point>();

    //view holder
    //CameraBridgeViewBase cameraView;
    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    //private CameraBridgeViewBase mOpenCvCameraView;
    PortraitCameraView cameraView;

    //camera listener callback
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG,"Loader interface success");

                    cameraView.enableView();

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG,String.valueOf(OpenCVLoader.initDebug()));

        //cameraView = (JavaCameraView) findViewById(R.id.frameView);
        cameraView = (PortraitCameraView) findViewById(R.id.frameView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        cameraView.setMaxFrameSize(640,480);
        cameraView.enableFpsMeter();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //mRgba = new Mat(height, width, CvType.CV_8UC4);
        //mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        //mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        //Mat gray = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        //gray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //Mat gray = inputFrame.gray();
        Mat gray = new Mat(width, width, CvType.CV_8UC4);
        Mat rgba = inputFrame.rgba();
        Mat thresh = new Mat();
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        //Log.d(TAG,String.valueOf(rgba.width()));
        //Log.d(TAG,String.valueOf(rgba.height()));

        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(gray, thresh,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 4);
        Mat hierarchy = new Mat();
        Core.bitwise_not(thresh,thresh);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        hierarchy.release();

        List<Rect> boxes = new ArrayList<Rect>();

        int sumx = 0, sumy = 0, sumw = 0, sumh = 0;

        for (MatOfPoint cnt : contours){

            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
            Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true);
            //int numberVertices = (int) approxCurve.total();

            if ((int) approxCurve.total() == 4 && Imgproc.arcLength(curve,true) > 50 && Imgproc.contourArea(cnt) > 500){

                Rect rect = Imgproc.boundingRect(cnt);

                if (rect.x > dyn_xmin && rect.x < dyn_xmax && rect.y > dyn_ymin && rect.y < dyn_ymax){

                    boxes.add(rect);
                    sumx = sumx + rect.x;
                    sumy = sumy + rect.y;
                    sumw = sumw + rect.width;
                    sumh = sumh + rect.height;

                    Imgproc.rectangle(rgba, new Point(rect.x,rect.y), new Point(rect.x + rect.width, rect.y + rect.height), red, 2);
                }
            }
        }

        if (boxes.size() > 10){

            cx = (sumx + sumw) / boxes.size() - sumw / (2 * boxes.size());
            cy = (sumy + sumh) / boxes.size() - sumh / (2 * boxes.size());
            width = sumw / boxes.size();
            height = sumh / boxes.size();
            dyn_xmin = cx - 5 * width;
            dyn_xmax = cx + 5 * width;
            dyn_ymin = cy - 5 * height;
            dyn_ymax = cy + 5 * height;
        }

        Imgproc.rectangle(rgba, new Point(dyn_xmin, dyn_ymin), new Point(dyn_xmax, dyn_ymax), blue, 2);

        ArrayList<ArrayList<Object>> symbols = new ArrayList<ArrayList<Object>>();

        for (MatOfPoint cnt : contours){

            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
            Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true);

            if ((int) approxCurve.total() == 4 && Imgproc.arcLength(curve,true) > 25 && Imgproc.arcLength(curve,true) < 500 && Imgproc.contourArea(cnt) > 25 && Imgproc.contourArea(cnt) < 500){

                Rect rect = Imgproc.boundingRect(cnt);

                if (rect.x > dyn_xmin && rect.x < dyn_xmax && rect.y > dyn_ymin && rect.y < dyn_ymax){

                    points = cnt.toList();

                    points.sort(Comparator.comparing(point -> point.x));
                    Point leftmost = points.get(0);
                    Point rightmost = points.get(points.size() - 1);

                    points.sort(Comparator.comparing(point -> point.y));
                    Point topmost = points.get(0);
                    Point bottomost = points.get(points.size() - 1);

                    int t = 4;

                    if ((Math.abs(rightmost.x-topmost.x) + Math.abs(rightmost.x-bottomost.x)) < t && Math.abs(leftmost.x-topmost.x) > t ) {
                        //System.out.println("LEFT");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"L")));
                        Imgproc.putText(rgba,"L",topmost,fontface,1, blue,2);
                    } else if ((Math.abs(leftmost.x-topmost.x) + Math.abs(leftmost.x-bottomost.x)) < t && Math.abs(leftmost.x-rightmost.x) > t) {
                        //System.out.println("RIGHT");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"R")));
                        Imgproc.putText(rgba,"R",topmost,fontface,1,blue,2);
                    } else if ((Math.abs(topmost.y-rightmost.y) + Math.abs(topmost.y-leftmost.y)) < t && Math.abs(topmost.y - bottomost.y) > t) {
                        //System.out.println("DOWN");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"D")));
                        Imgproc.putText(rgba,"D",topmost,fontface,1,blue,2);
                    } else if ((Math.abs(bottomost.y-rightmost.y) + Math.abs(bottomost.y-leftmost.y)) < t && Math.abs(topmost.y - bottomost.y) > t) {
                        //System.out.println("UP");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"U")));
                        Imgproc.putText(rgba,"U",topmost,fontface,1,blue,2);
                    }

                    Imgproc.rectangle(rgba, new Point(rect.x,rect.y), new Point(rect.x + rect.width, rect.y + rect.height), red, 2);

                }
            }
        }

        ArrayList<ArrayList<Object>> numbers = new ArrayList<ArrayList<Object>>();

        for (Rect box : boxes){

            contours.clear();
            Imgproc.findContours(thresh.submat(box), contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE, new Point(box.x,box.y));

            List<MatOfPoint> cnt_approx = new ArrayList<MatOfPoint>();

            for (MatOfPoint cnt : contours){
                MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());

                if (Imgproc.arcLength(curve,true) > 50 && Imgproc.arcLength(curve,true) < 300 && Imgproc.contourArea(cnt) > 50 && Imgproc.contourArea(curve) < 300){

                    Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true);

                    MatOfPoint mPoints = new MatOfPoint();
                    mPoints.fromList(approxCurve.toList());
                    cnt_approx.clear();
                    cnt_approx.add(mPoints);

                    Imgproc.drawContours(rgba,cnt_approx,-1, blue,2);

                    points = cnt.toList();

                    points.sort(Comparator.comparing(point -> point.x));
                    Point leftmost = points.get(0);
                    Point rightmost = points.get(points.size() - 1);

                    points.sort(Comparator.comparing(point -> point.y));
                    Point topmost = points.get(0);
                    Point bottomost = points.get(points.size() - 1);


                    if (approxCurve.size(0) == 2){
                        //System.out.println("3");
                        Imgproc.putText(rgba,"3",topmost,fontface,1, green,2);
                        Rect rect = Imgproc.boundingRect(cnt);
                        numbers.add(new ArrayList<>(Arrays.asList(rect,"3")));
                    } else if (approxCurve.size(0) > 2 && (Math.abs(leftmost.y-bottomost.y) < 4 || Math.abs(rightmost.y-bottomost.y) < 4)) {
                        //System.out.println("2");
                        Imgproc.putText(rgba,"2",topmost,fontface,1, green,2);
                        Rect rect = Imgproc.boundingRect(cnt);
                        numbers.add(new ArrayList<>(Arrays.asList(rect,"2")));
                    } else if(approxCurve.size(0) > 2 && Math.abs(leftmost.y-bottomost.y) > 4){
                        //System.out.println("4");
                        Imgproc.putText(rgba,"4",topmost,fontface,1, green,2);
                        Rect rect = Imgproc.boundingRect(cnt);
                        numbers.add(new ArrayList<>(Arrays.asList(rect,"4")));
                    }
                }
            }
        }

        if (Process == false) {
            int i = 0;
            for (Rect square : squares){
                Imgproc.putText(rgba,sol.substring(i,i+1),new Point(square.x + square.width/2, square.y + square.height/2),fontface,1, green,2);
                i += 1;
            }
        }

        //Log.d(TAG, "number of boxes: " + boxes.size());

        if (boxes.size() == 25){
            squares = getSquares(boxes);
            update_row_dist(symbols);
            sym_row_norm = gen_row_norm(sym_row_dist);
            update_col_dist(symbols);
            sym_col_norm = gen_col_norm(sym_col_dist);
            update_number_dist(numbers);
            num_norm = gen_num_norm(n_dist);

            Log.d(TAG, "row dist: " + sym_row_norm);
            Log.d(TAG, "col dist: " + sym_col_norm);
            Log.d(TAG, "num dist: " + num_norm);

            if ( sym_row_norm.stream().map(s -> Collections.max(s)).filter(s -> s < 75).count() == 0 &&
                    sym_row_norm.stream().map(s -> Collections.max(s)).filter(s -> s < 75).count() == 0 &&
                    num_norm.stream().map(s -> Collections.max(s)).filter(s -> s < 75).count() == 0 &&
                    Arrays.stream(n_dist).flatMapToInt(Arrays::stream).sum() > 50 &&
                    Process == true){

                StringBuilder sb = new StringBuilder();

                num_weighted = get_num_weighted(n_dist);

                Log.d(TAG, "num weighted: " + num_weighted);

                int[][] puzzle = new int[5][5];
                for (int row = 0; row < 5; row++){
                    for (int col = 0; col < 5; col++){
                        puzzle[row][col] = num_weighted.get(row * 5 + col);
                        sb.append(puzzle[row][col]);
                        if (col < 4){
                            sb.append(',');
                        }
                    }
                    sb.append('\n');
                }
                //System.out.println(Arrays.deepToString(puzzle));

                weighted = get_weighted(sym_row_dist);

                int[][] arr = new int[5][4];
                for (int row = 0; row < 5; row++){
                    for (int col = 0; col < 4; col++){
                        arr[row][col] = weighted.get(row * 4 + col);
                        sb.append(arr[row][col]);
                        if (col < 3){
                            sb.append(',');
                        }
                    }
                    sb.append('\n');
                }
                //System.out.println(Arrays.deepToString(arr));

                weighted = get_weighted(sym_col_dist);

                int[][] arr1 = new int[5][4];
                for (int row = 0; row < 5; row++){
                    for (int col = 0; col < 4; col++){
                        arr1[row][col] = weighted.get(row * 4 + col);
                        sb.append(arr1[row][col]);
                        if (col < 3){
                            sb.append(',');
                        }
                    }
                    sb.append('\n');
                }

                Log.d(TAG, "puzzle detected: " + sb.toString());

                try{
                    URL urlObj = new URL("https://futoshiki-solver.herokuapp.com/");
                    HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");

                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);

                    conn.connect();
                    String paramsString = sb.toString();

                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.writeBytes(paramsString);
                    wr.flush();
                    wr.close();

                    try {
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        //StringBuilder sol = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sol.append(line);
                        }

                        Log.d(TAG, "result from server: " + sol.toString());

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (conn != null) {
                            conn.disconnect();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


/*                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        //.uri(URI.create("http://127.0.0.1:8000/"))
                        .uri(URI.create("https://futoshiki-solver.herokuapp.com/"))
                        .POST(BodyPublishers.ofString(sb.toString()))
                        .build();

                HttpResponse<String> response =
                        client.send(request, BodyHandlers.ofString());

                sol = response.body();*/

                //System.out.println(sol);

                if (sol.length() > 0){
                    Process = false;
                }

                for (int i = 0; i < sym_row_dist.length; i++) { for (int j = 0; j < sym_row_dist[i].length; j++) { sym_row_dist[i][j] = 0; } }
                for (int i = 0; i < sym_col_dist.length; i++) { for (int j = 0; j < sym_col_dist[i].length; j++) { sym_col_dist[i][j] = 0; } }
                for (int i = 0; i < n_dist.length; i++) { for (int j = 0; j < n_dist[i].length; j++) { n_dist[i][j] = 0; } }
            }
        } else {

            for (int i = 0; i < sym_row_dist.length; i++) { for (int j = 0; j < sym_row_dist[i].length; j++) { sym_row_dist[i][j] = 0; } }
            for (int i = 0; i < sym_col_dist.length; i++) { for (int j = 0; j < sym_col_dist[i].length; j++) { sym_col_dist[i][j] = 0; } }
            for (int i = 0; i < n_dist.length; i++) { for (int j = 0; j < n_dist[i].length; j++) { n_dist[i][j] = 0; } }
            Process = true;

        }

        return rgba;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "There is a problem", Toast.LENGTH_SHORT).show();
        } else {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    private static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    private void setLabel(Mat im, String label, MatOfPoint contour) {
        int fontface = Core.FONT_HERSHEY_SIMPLEX;
        double scale = 3;//0.4;
        int thickness = 3;//1;
        int[] baseline = new int[1];
        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);
        Rect r = Imgproc.boundingRect(contour);
        Point pt = new Point(r.x + ((r.width - text.width) / 2),r.y + ((r.height + text.height) / 2));
        Imgproc.putText(im, label, pt, fontface, scale, new Scalar(255, 0, 0), thickness);
    }

    public static List<Rect> getSquares(List<Rect> boxes) {
        // CB 25-11-21
        // method takes 25 boxes and maps to 25 squares where square 0 is top left and square 24 is bottom right
        // boxes have no inherent or reliable ordering when deteced using opencv find contours

        List<Rect> squares = new ArrayList<Rect>();
        List<Rect> row = new ArrayList<Rect>();

        //sort by y i.e. row order
        boxes.sort(Comparator.comparing(point -> point.y));

        // grab each row of 5, then order by x, finally add to new list
        for (int i = 0; i <= 20; i += 5){
            row = boxes.subList(i, i + 5);
            row.sort(Comparator.comparing(point -> point.x));
            for (Rect r : row) {
                squares.add(r);
            }
        }
        return squares;
    }

    public static void update_row_dist(ArrayList<ArrayList<Object>> symbols){
        for (ArrayList<Object> symbol:symbols){
            for (int row = 0; row < 5; row++){
                for (int col = 0; col < 4; col++){
                    int squareID = row * 5 + col;
                    Rect r = (Rect)symbol.get(0);
                    if (r.x > (squares.get(squareID).x + squares.get(squareID).width) &&
                            r.x < squares.get(squareID + 1).x &&
                            r.y > squares.get(squareID).y &&
                            r.y < (squares.get(squareID).y + squares.get(squareID).height)
                    ){
                        String s = (String)symbol.get(1);
                        if (s == "L"){
                            //System.out.println(s);
                            sym_row_dist[row * 4 + col][0] += 1;
                        }
                        else{
                            //System.out.println(s);
                            sym_row_dist[row * 4 + col][1] += 1;
                        }
                    }
                }
            }
        }
    }

    public static void update_col_dist(ArrayList<ArrayList<Object>> symbols){
        for (ArrayList<Object> symbol:symbols){
            for (int col = 0; col < 5; col++){
                for (int row = 0; row < 4; row++){
                    int squareID = row * 5 + col;
                    Rect r = (Rect)symbol.get(0);
                    //if (symbol[0] > squares[square][0]) and (symbol[0] < (squares[square][0] + squares[square][2])):
                    //if (symbol[1] > (squares[square][1] + squares[square][3])) and symbol[1] < squares[square + 5][1]:
                    if (r.x > squares.get(squareID).x && r.x < (squares.get(squareID).x + squares.get(squareID).width) &&
                            r.y > (squares.get(squareID).y + squares.get(squareID).height) && r.y < squares.get(squareID + 5).y
                    ){
                        String s = (String)symbol.get(1);
                        if (s == "U"){
                            //System.out.println(s);
                            sym_col_dist[col * 4 + row][0] += 1;
                        }
                        else{
                            //System.out.println(s);
                            sym_col_dist[col * 4 + row][1] += 1;
                        }
                    }
                }
            }
        }
    }

    public static void update_number_dist(ArrayList<ArrayList<Object>> numbers){
        for (ArrayList<Object> number:numbers){
            for (int row = 0; row < 5; row++){
                for (int col = 0; col < 5; col++){
                    int squareID = row * 5 + col;
                    Rect r = (Rect)number.get(0);
                    if (r.x < (squares.get(squareID).x + squares.get(squareID).width) && r.x > squares.get(squareID ).x &&
                            r.y > squares.get(squareID).y && r.y < (squares.get(squareID).y + squares.get(squareID).height)
                    ){
                        String s = (String)number.get(1);
                        int n = Integer.parseInt(s);
                        //System.out.println(s);
                        n_dist[squareID][n] += 1;

                    }
                }
            }
        }
    }

    public static ArrayList<ArrayList<Double>> gen_row_norm(int sym_row_dist[][]){
        ArrayList<ArrayList<Double>> sym_norm = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < 5; row++){
            for (int col = 0; col < 4; col++){
                int s1 = sym_row_dist[row * 4 + col][0];
                int s2 = sym_row_dist[row * 4 + col][1];
                if ( s1 > 0 || s2 > 0 ){
                    ArrayList<Double> test = new ArrayList<Double>();
                    double d1 = s1 * 100 / (s1 + s2);
                    test.add(d1);
                    double d2 = s2 * 100 / (s1 + s2);
                    test.add(d2);
                    sym_norm.add(test);
                }
            }
        }
        return sym_norm;
    }

    public static ArrayList<ArrayList<Double>> gen_col_norm(int sym_col_dist[][]){
        ArrayList<ArrayList<Double>> sym_col_norm = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < 5; row++){
            for (int col = 0; col < 4; col++){
                int s1 = sym_col_dist[row * 4 + col][0];
                int s2 = sym_col_dist[row * 4 + col][1];
                if ( s1 > 0 || s2 > 0 ){
                    ArrayList<Double> test = new ArrayList<Double>();
                    double d1 = s1 * 100 / (s1 + s2);
                    test.add(d1);
                    double d2 = s2 * 100 / (s1 + s2);
                    test.add(d2);
                    sym_col_norm.add(test);
                }
            }
        }
        return sym_col_norm;
    }



    public static ArrayList<ArrayList<Double>> gen_num_norm(int n_dist[][]){
        ArrayList<ArrayList<Double>> num_norm = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < 25; row++){
            int sum = 0;
            for (int col = 0; col < 5; col++){
                sum += n_dist[row][col];
            }
            if ( sum > 0 ){
                ArrayList<Double> test = new ArrayList<Double>();
                for (int col = 0; col < 5; col++){
                    // normalise each value using total number
                    double d1 = n_dist[row][col] * 100 / sum;
                    test.add(d1);
                }
                num_norm.add(test);
            }
        }

        return num_norm;
    }


    public static List<Integer> get_weighted(int sym_row_dist[][]){
        List<Integer> weighted = Arrays.stream(sym_row_dist)
                //.map(m -> Arrays.stream(m).reduce((a,b) -> m[a]<m[b]? a: b))
                .map(m -> {
                    Integer result = 0;
                    if (m[0] > m[1]){
                        result = 1;
                    } else if (m[0] < m[1]){
                        result = 2;
                    }
                    return result;
                })
                .collect(Collectors.toList());

        return weighted;
    }

    public static List<Integer> get_num_weighted(int n_dist[][]){
        List<Integer> num_weighted = Arrays.stream(n_dist)
                //.map(m -> Arrays.stream(m).reduce((a,b) -> m[a]<m[b]? a: b))
                .map(m -> {
                    int index_of_max = IntStream.range(0, 5)
                            .reduce(0,(a,b)->m[a]<m[b]? b: a);
                    //.ifPresent(ix->System.out.println("Index "+ix+", value "+m[ix]));
                    return index_of_max;
                })
                .collect(Collectors.toList());
        return num_weighted;
    }
}
