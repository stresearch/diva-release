package com.visym.collector.capturemodule.presenters;

import android.content.Context;
import android.text.TextUtils;

import com.visym.collector.capturemodule.IVideoEditModule;
import com.visym.collector.capturemodule.IVideoEditModule.IObjectEditing;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameMetaData;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.model.BoundingBox;
import com.visym.collector.model.Frame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ObjectEditPresenter implements IObjectEditing {
    @Override
    public void onViewAttached(IVideoEditModule.IVideoEditView view, Context context) {

    }

    @Override
    public void onViewDetached() {

    }

    @Override
    public void updateEditedObject(String selectedItem, FrameJSON copyFrame,
                                   HashMap<Integer, Frame> boundingBoxes, int originalWidth, int originalHeight,
                                   int currentWidth, int currentHeight, int x, int y) {
        if (copyFrame != null) {
            FrameMetaData metaData = copyFrame.getMetaData();
            if (metaData == null) {
                return;
            }
            int frameWidth = metaData.getFrameWidth();
            int frameHeight = metaData.getFrameHeight();
            boolean objectFound = false;

            List<FrameObject> object = copyFrame.getObject();
            if (object != null && !object.isEmpty()) {
                for (FrameObject frameObject : object) {
                    String label = frameObject.getLabel();
                    if (!TextUtils.isEmpty(label) && label.equalsIgnoreCase(selectedItem)) {
                        objectFound = true;
                        List<BoundingBox> boxes = new ArrayList<>();
                        for (Integer key : boundingBoxes.keySet()) {
                            Frame frame = boundingBoxes.get(key);
                            if (frame != null) {
                                int width = (frameWidth * frame.getWidth()) / currentWidth;
                                int height = (frameHeight * frame.getHeight()) / currentHeight;

                                int newX = (frameWidth * frame.getX()) / currentWidth;
                                int newY = (frameHeight * frame.getY()) / currentHeight;

                                Frame newFrame = new Frame(newX, newY, width, height);
                                newFrame.setDefault(frame.isDefault());
                                boxes.add(new BoundingBox(newFrame, key));
                            }
                        }
                        frameObject.setBoundingBox(boxes);
                    }
                }
            }
            if (!objectFound && selectedItem != null) {
                if (object == null) {
                    object = new ArrayList<>();
                }
                List<BoundingBox> boxes = new ArrayList<>();
                for (Integer key : boundingBoxes.keySet()) {
                    Frame frame = boundingBoxes.get(key);
                    if (frame != null) {
                        int width = (frameWidth * frame.getWidth()) / currentWidth;
                        int height = (frameHeight * frame.getHeight()) / currentHeight;

                        int newX = (frameWidth * frame.getX()) / currentWidth;
                        int newY = (frameHeight * frame.getY()) / currentHeight;

                        Frame newFrame = new Frame(newX, newY, width, height);
                        newFrame.setDefault(frame.isDefault());
                        boxes.add(new BoundingBox(newFrame, key));
                    }
                }
                FrameObject frameObject = new FrameObject(selectedItem, boxes);
                object.add(frameObject);
            }
        }
        else {
            return; // JEBYRNE: should never get here
        }
    }

    @Override
    public List<BoundingBox> getObject(String selectedItem, FrameJSON copyFrame) {
        if (copyFrame != null) {
            List<FrameObject> frameObjects = copyFrame.getObject();
            if (frameObjects != null && !frameObjects.isEmpty()) {
                for (FrameObject frameObject : frameObjects) {
                    String label = frameObject.getLabel();
                    if (!TextUtils.isEmpty(label) && label.equalsIgnoreCase(selectedItem)) {
                        return frameObject.getBoundingBox();
                    }
                }
            }
        }
        else {
            // JEBYRNE: should never get here
        }
        return null;
    }

    @Override
    public Frame getConvertedFrame(int frameWidth, int frameHeight, int videoWidth,
                                   int videoHeight, Frame frame) {
        int width = (videoWidth * frame.getWidth()) / frameWidth;
        int height = (videoHeight * frame.getHeight()) / frameHeight;

        int newX = (videoWidth * frame.getX()) / frameWidth;
        int newY = (videoHeight * frame.getY()) / frameHeight;

        Frame newFrame = new Frame(newX, newY, width, height);
        newFrame.setDefault(frame.isDefault());
        return newFrame;
    }

    @Override
    public HashMap<Integer, Frame> getObjectBoxes(String selectedItem, FrameJSON copyFrame,
                                                  int currentWidth, int currentHeight) {
        if (copyFrame == null || (copyFrame.getMetaData() == null)) {
            return null;
        }
        HashMap<Integer, Frame> boxes = new LinkedHashMap<>();
        int frameWidth = copyFrame.getMetaData().getFrameWidth();
        int frameHeight = copyFrame.getMetaData().getFrameHeight();

        List<BoundingBox> objects = getObject(selectedItem, copyFrame);
        if (objects != null && objects.size() > 0) {
            for (BoundingBox object : objects) {
                Frame newFrame = getConvertedFrame(frameWidth, frameHeight, currentWidth,
                        currentHeight, object.getFrame());
                boxes.put(object.getFrameIndex(), newFrame);
            }
        }
        return boxes;
    }

    @Override
    public void deleteObjectFrames(String selectedItem, List<FrameObject> object) {
        if (object != null && !object.isEmpty()) {
            for (FrameObject frameObject : object) {
                if (frameObject.getLabel().equalsIgnoreCase(selectedItem)) {
                    object.remove(frameObject);
                    break;
                }
            }
        }
    }

    public HashMap<Integer, List<Frame>>
    generateObjectFrames(FrameJSON copyFrame, int videoWidth, int videoHeight, String defaultObjectName) {
        HashMap<Integer, List<Frame>> objectFrames = new LinkedHashMap<>();
        if (copyFrame != null) {
            FrameMetaData metaData = copyFrame.getMetaData();
            if (metaData == null) {
                return null;
            }
            int frameWidth = metaData.getFrameWidth();
            int frameHeight = metaData.getFrameHeight();

            List<FrameObject> objects = copyFrame.getObject();
            if (objects != null && !objects.isEmpty()) {
                for (int i = 0; i < objects.size(); i++) {
                    FrameObject object = objects.get(i);
                    List<BoundingBox> boundingBox = object.getBoundingBox();
                    if (boundingBox != null && !boundingBox.isEmpty()) {
                        for (BoundingBox box : boundingBox) {
                            List<Frame> frames = objectFrames.get(box.getFrameIndex());
                            if (frames == null) {
                                frames = new ArrayList<>();
                            }
                            Frame frame = box.getFrame();
                            Frame newFrame = getConvertedFrame(frameWidth, frameHeight,
                                    videoWidth, videoHeight, frame);
                            if (defaultObjectName != null && defaultObjectName.contentEquals(object.getLabel())) {
                                newFrame.setDefault(true);
                                frame.setDefault(true);
                            }
                            frames.add(newFrame);
                            objectFrames.put(box.getFrameIndex(), frames);
                        }
                    }
                }
            }
        }
        else {
            return null;  // JEBYRNE: should never get here
        }
        return objectFrames;
    }
}
