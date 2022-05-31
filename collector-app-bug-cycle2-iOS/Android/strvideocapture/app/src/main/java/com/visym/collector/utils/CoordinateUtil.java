package com.visym.collector.utils;

import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.visym.collector.model.Frame;

import java.util.List;

public class CoordinateUtil {
    /**
     * Given two points in the plane p1=(x1, x2) and p2=(y1, y1), this method
     * returns the direction that an arrow pointing from p1 to p2 would have.
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the direction
     */
    public static Direction getDirection(float x1, float y1, float x2, float y2){
        double angle = getAngle(x1, y1, x2, y2);
        Direction direction = Direction.fromAngle(angle);
        Log.d("Camera", "getDirection: angle " + angle + " " + direction.name());
        return direction;
    }

    /**
     *
     * Finds the angle between two points in the plane (x1,y1) and (x2, y2)
     * The angle is measured with 0/360 being the X-axis to the RIGHT, angles
     * increase counter clockwise.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the angle between two points
     */
    public static double getAngle(float x1, float y1, float x2, float y2) {
        double rad = Math.atan2(y1-y2,x2-x1) + Math.PI;
        return (rad*180/Math.PI + 180)%360;
    }

    public static int getCorner(int x, int y, int screenWidth, int screenHeight) {
        int middleWidth = screenWidth / 2;
        int middleHeight = screenHeight / 2;
        Log.d("TAG", "onTouch: "+ x+ " " + y + " " + middleWidth + " " + middleHeight);
        if (x < middleWidth && y < middleHeight){
            return 1;
        }else if (x > middleWidth && y < middleHeight){
            return 2;
        }else  if (x > middleWidth && y > middleHeight){
            return 3;
        }else {
            return 4;
        }
    }

    public static boolean checkPointsTouchesBorder(int x, int y, int corner, int boxWidth, int boxHeight,
                                                   int displayWidth, int displayHeight) {
        int i, j;
        if (corner == 1){
            i = (displayWidth - boxWidth) / 2;
            j = (displayHeight - boxHeight) / 2;
        }
        else if (corner == 2){
            i = displayWidth - ((displayWidth - boxWidth) / 2);
            j = (displayHeight - boxHeight) / 2;
        }
        else if (corner == 3){
            i = displayWidth - ((displayWidth - boxWidth) / 2);
            j = displayHeight - ((displayHeight - boxHeight) / 2);
        }
        else {
            i = (displayWidth - boxWidth) / 2;
            j = displayHeight - ((displayHeight - boxHeight) / 2);
        }
        int x1 = i, x2 = i;
        int y1 = j, y2 = j;
        return (x > x1 && x < x2) && (y > y1 && y < y2);
    }

    public static boolean checkTouchPointsInsideBox(MotionEvent event, int x1, int y1,
                                                    int width, int height) {
        int x = (int) Math.abs(event.getX());
        int y = (int) Math.abs(event.getY());

        int x2 = x1 + width;
        int y3 = y1 + height;
        return x > x1 && x < x2 && y > y1 && y < y3;
    }

    public enum Direction{
        UP,
        DOWN,
        LEFT,
        RIGHT,
        DIAGONAL;

        /**
         * Returns a direction given an angle.
         * Directions are defined as follows:
         *
         * Up: [45, 135]
         * Right: [0,45] and [315, 360]
         * Down: [225, 315]
         * Left: [135, 225]
         * DIAGONAL [apart from above angles]
         *
         * @param angle an angle from 0 to 360 - e
         * @return the direction of an angle
         */
        public static Direction fromAngle(double angle){
//            if (inRange(angle, 0, 45) || inRange(angle, 315, 360)){
//                return Direction.RIGHT;
//            }else if (inRange(angle, 135, 225)){
//                return Direction.LEFT;
//            }else if (inRange(angle, 45, 135)){
//                return Direction.UP;
//            }else if(inRange(angle, 225, 315)){
//                return Direction.DOWN;
//            }
//            else {
//                return Direction.DIAGONAL;
//            }

            if(inRange(angle,45, 135)){
                return Direction.UP;
            }
            else if(inRange(angle,0,45) || inRange(angle, 315, 360)){
                return Direction.RIGHT;
            }
            else if(inRange(angle,225, 315)){
                return Direction.DOWN;
            }
            else if(inRange(angle,135, 225)){
                return Direction.LEFT;
            }
            else {
                return Direction.DIAGONAL;
            }
        }

        /**
         * @param angle an angle
         * @param init the initial bound
         * @param end the final bound
         * @return returns true if the given angle is in the interval [init, end).
         */
        private static boolean inRange(double angle, float init, float end){
            return (angle >= init) && (angle < end);
        }
    }
}
