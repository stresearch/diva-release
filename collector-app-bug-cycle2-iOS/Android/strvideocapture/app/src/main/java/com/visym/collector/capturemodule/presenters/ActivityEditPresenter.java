package com.visym.collector.capturemodule.presenters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.google.gson.Gson;
import com.visym.collector.R;
import com.visym.collector.capturemodule.IVideoEditModule;
import com.visym.collector.capturemodule.IVideoEditModule.IActivityEdit;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.model.BoundingBox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ActivityEditPresenter implements IActivityEdit {

    private IVideoEditModule.IVideoEditView mview;
    private Context mcontext;

    @Override
    public void onViewAttached(IVideoEditModule.IVideoEditView view, Context context) {
        this.mview = view;
        this.mcontext = context;
    }

    @Override
    public void onViewDetached() {
        this.mview = null;
    }

    @Override
    public JSONArray getBoundingBoxesGap(FrameJSON copyFrame, double frameDuration, int videoDuration)
            throws JSONException {
        JSONArray newArray = new JSONArray();
        if (copyFrame != null) {
            List<FrameObject> objects = copyFrame.getObject();
            if (objects != null && !objects.isEmpty()) {
                List<BoundingBox> boundingBoxes = objects.get(0).getBoundingBox();
                if (boundingBoxes != null && !boundingBoxes.isEmpty()) {
                    int startFrameIndex = 0, currentValue = 0;
                    for (int i = 0; i < boundingBoxes.size(); i++) {
                        BoundingBox boundingBox = boundingBoxes.get(i);
                        if (i == 0) {
                            startFrameIndex = boundingBox.getFrameIndex();
                            if (boundingBox.getFrameIndex() > 0) {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("startFrame", 0);
                                jsonObject.put("endFrame", Math.round(startFrameIndex * frameDuration - 1));
                                jsonObject.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                                jsonObject.put("activityLabel", "empty");
                                newArray.put(jsonObject);
                            }
                            currentValue = startFrameIndex;
                            currentValue++;
                        } else if (currentValue == boundingBox.getFrameIndex()) {
                            currentValue++;
                        } else {
                            BoundingBox previousBox = boundingBoxes.get(i - 1);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("startFrame", Math.round(startFrameIndex * frameDuration));
                            jsonObject.put("endFrame", Math.round(previousBox.getFrameIndex() * frameDuration));
                            jsonObject.put("color", mcontext.getResources().getColor(R.color.transparent));
                            jsonObject.put("activityLabel", "empty");
                            newArray.put(jsonObject);

                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("startFrame", Math.round(previousBox.getFrameIndex() * frameDuration + 1));
                            jsonObject1.put("endFrame", Math.round(currentValue * frameDuration - 1));
                            jsonObject1.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                            jsonObject1.put("activityLabel", "empty");
                            newArray.put(jsonObject1);

                            currentValue = boundingBox.getFrameIndex();
                        }

                        if (i == boundingBoxes.size() - 1) {
                            int lastIndex = (int) Math.round(boundingBox.getFrameIndex() * frameDuration);
                            if (lastIndex < videoDuration) {
                                if (newArray.length() == 1) {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("startFrame", Math.round(startFrameIndex * frameDuration));
                                    jsonObject.put("endFrame", Math.round(boundingBox.getFrameIndex() * frameDuration));
                                    jsonObject.put("color", mcontext.getResources().getColor(R.color.transparent));
                                    jsonObject.put("activityLabel", "empty");
                                    newArray.put(jsonObject);
                                } else if (newArray.length() == 0) {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("startFrame", Math.round(startFrameIndex * frameDuration));
                                    jsonObject.put("endFrame", Math.round(boundingBox.getFrameIndex() * frameDuration));
                                    jsonObject.put("color", mcontext.getResources().getColor(R.color.transparent));
                                    jsonObject.put("activityLabel", "empty");
                                    newArray.put(jsonObject);
                                }

                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("startFrame", lastIndex + 1);
                                jsonObject.put("endFrame", videoDuration);
                                jsonObject.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                                jsonObject.put("activityLabel", "empty");
                                newArray.put(jsonObject);
                            }
                        }
                    }
                }
            }
        }
        return newArray;
    }

    @Override
    public void deleteActivity(String selectedItem, FrameJSON copyFrame) {
        List<ActivityLabel> activities = copyFrame.getActivity();
        if (activities != null && !activities.isEmpty()) {
            Iterator<ActivityLabel> iterator = activities.iterator();
            while (iterator.hasNext()) {
                ActivityLabel activity = iterator.next();
                if (activity.getLabel().equalsIgnoreCase(selectedItem)) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public JSONArray getActivityValues(FrameJSON copyFrame, double frameDuration, int videoDuration) throws JSONException {
        JSONArray activityArray = new JSONArray();
        List<ActivityLabel> activities = copyFrame.getActivity();
        if (activities != null && !activities.isEmpty()) {
            for (ActivityLabel activity : activities) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("startFrame", Math.round(activity.getStartFrame() * frameDuration));
                jsonObject.put("endFrame", Math.round(activity.getEndFrame() * frameDuration));
                jsonObject.put("activityLabel", activity.getLabel());
                jsonObject.put("color", mcontext.getResources().getColor(R.color.activityColor));
                activityArray.put(jsonObject);
            }
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("startFrame", 0);
            jsonObject.put("endFrame", videoDuration);
            jsonObject.put("activityLabel", "empty");
            jsonObject.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
            activityArray.put(jsonObject);
        }
        return activityArray;
    }

    public String getActivityLabels(int frameIndex, FrameJSON copyFrame) {
        StringBuilder activityLabel = null;
        List<ActivityLabel> activities = copyFrame.getActivity();
        if (activities != null && !activities.isEmpty()) {
            for (ActivityLabel activity : activities) {
                if (frameIndex >= activity.getStartFrame() && frameIndex <= activity.getEndFrame()) {
                    if (activityLabel == null) {
                        activityLabel = new StringBuilder(activity.getLabel());
                    } else {
                        if (!activityLabel.toString().contains(activity.getLabel())) {
                            activityLabel.append("\n").append(activity.getLabel());
                        }
                    }
                }
            }
        }
        if (activityLabel != null) {
            return activityLabel.toString();
        }
        return null;
    }

    @Override
    public JSONArray updateTempArray(String selectedItem, JSONArray tempArray, int startFrame,
                                     int endFrame, double frameDuration, double videoDuration)
            throws JSONException {
        boolean activityFound = false;
        if (tempArray != null && tempArray.length() > 0) {
            for (int i = 0; i < tempArray.length(); i++) {
                JSONObject jsonObject = tempArray.getJSONObject(i);
                if (jsonObject.getString("activityLabel").equalsIgnoreCase(selectedItem)) {
                    activityFound = true;
                    break;
                }
            }
        }
        if (!activityFound && tempArray != null) {
            if (startFrame == 0) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("startFrame", startFrame);
                jsonObject.put("endFrame", endFrame);
                jsonObject.put("activityLabel", selectedItem);
                jsonObject.put("color", mcontext.getResources().getColor(R.color.activityColor));
                tempArray.put(jsonObject);

                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("startFrame", endFrame + 1);
                jsonObject1.put("endFrame", videoDuration);
                jsonObject1.put("activityLabel", "empty");
                jsonObject1.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                tempArray.put(jsonObject1);
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("startFrame", 0);
                jsonObject.put("endFrame", startFrame - 1);
                jsonObject.put("activityLabel", "empty");
                jsonObject.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                tempArray.put(jsonObject);

                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("startFrame", startFrame + 1);
                jsonObject1.put("endFrame", endFrame);
                jsonObject1.put("activityLabel", selectedItem);
                jsonObject1.put("color", mcontext.getResources().getColor(R.color.activityColor));
                tempArray.put(jsonObject1);

                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("startFrame", endFrame + 1);
                jsonObject2.put("endFrame", videoDuration);
                jsonObject2.put("activityLabel", "empty");
                jsonObject2.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                tempArray.put(jsonObject2);
            }
        }
        return tempArray;
    }

    @Override
    public JSONArray getActivitySegments(List<ActivityLabel> activities,
                                         double frameDuration, double videoDuration) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        try {
            if (activities != null && !activities.isEmpty()) {
                Collections.sort(activities);
                List<ActivityLabel> newActivities = new ArrayList<>();
                if (activities.size() == 1) {
                    newActivities.add(activities.get(0));
                }else {
                    for (int i = 0; i < activities.size(); i++) {
                        ActivityLabel currentActivity = activities.get(i);
                        for (int j = i + 1; j < activities.size(); j++) {
                            ActivityLabel nextActivity = activities.get(j);
                            if (currentActivity.getStartFrame() <= nextActivity.getStartFrame() &&
                                    currentActivity.getEndFrame() <= nextActivity.getStartFrame()) {
                                if (!newActivities.contains(currentActivity)) {
                                    if (!newActivities.isEmpty()){
                                        int endFrame = newActivities.get(newActivities.size() - 1).getEndFrame();
                                        if (endFrame < currentActivity.getEndFrame()) {
                                            newActivities.add(new ActivityLabel(currentActivity.getLabel(),
                                                    currentActivity.getStartFrame(), currentActivity.getEndFrame()));
                                        }
                                    }else {
                                        newActivities.add(new ActivityLabel(currentActivity.getLabel(),
                                                currentActivity.getStartFrame(), currentActivity.getEndFrame()));
                                    }
                                }
                            }
                            if (currentActivity.getStartFrame() <= nextActivity.getStartFrame()
                                    && currentActivity.getEndFrame() >= nextActivity.getEndFrame()) {
                                if (!newActivities.contains(currentActivity)) {
                                    if (!newActivities.isEmpty()) {
                                        int endFrame = newActivities.get(newActivities.size() - 1).getEndFrame();
                                        if (endFrame < currentActivity.getEndFrame()) {
                                            newActivities.add(new ActivityLabel(currentActivity.getLabel(),
                                                    currentActivity.getStartFrame(), currentActivity.getEndFrame()));
                                        }
                                    }else {
                                        newActivities.add(new ActivityLabel(currentActivity.getLabel(),
                                                currentActivity.getStartFrame(), currentActivity.getEndFrame()));
                                    }
                                }
                                i++;
                            } else if (currentActivity.getEndFrame() >= nextActivity.getStartFrame()
                                    && currentActivity.getEndFrame() <= nextActivity.getEndFrame()) {
                                int endFrame = currentActivity.getEndFrame();
                                currentActivity.setEndFrame(nextActivity.getEndFrame());
                                if (!newActivities.contains(currentActivity)) {
                                    newActivities.add(new ActivityLabel(currentActivity.getLabel(),
                                            currentActivity.getStartFrame(), currentActivity.getEndFrame()));
                                    currentActivity.setEndFrame(endFrame);
                                }
                                i++;
                            }
                        }
                        if (i > 0 && i == activities.size() - 1) {
                            ActivityLabel previousLabel = activities.get(i - 1);
                            if (currentActivity.getStartFrame() > previousLabel.getEndFrame()) {
                                int endFrame = newActivities.get(newActivities.size() - 1).getEndFrame();
                                if (endFrame < currentActivity.getEndFrame()) {
                                    newActivities.add(new ActivityLabel(currentActivity.getLabel(),
                                            currentActivity.getStartFrame(), currentActivity.getEndFrame()));
                                }
                            }
                        }
                    }
                }

                Log.d("TAG", "getActivitySegments: ");

                for (ActivityLabel newActivity : newActivities) {
                    Log.d("TAG", "getActivitySegments: "+ newActivity.getStartFrame() + " " + newActivity.getEndFrame());
                }

                for (int i = 0; i < newActivities.size(); i++) {
                    ActivityLabel activityLabel = newActivities.get(i);
                    int startFrame = activityLabel.getStartFrame();
                    int endFrame = activityLabel.getEndFrame();

                    if (i == 0) {
                        if (startFrame == 0) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("startFrame", Math.round(startFrame * frameDuration));
                            jsonObject.put("endFrame", Math.round(endFrame * frameDuration));
                            jsonObject.put("activityLabel", activityLabel.getLabel());
                            jsonObject.put("color", mcontext.getResources().getColor(R.color.activityColor));
                            jsonArray.put(jsonObject);
                        } else {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("startFrame", 0);
                            jsonObject.put("endFrame", Math.round(startFrame * frameDuration) - 1);
                            jsonObject.put("activityLabel", "empty");
                            jsonObject.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                            jsonArray.put(jsonObject);

                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("startFrame", Math.round(startFrame * frameDuration));
                            jsonObject1.put("endFrame", Math.round(endFrame * frameDuration));
                            jsonObject1.put("activityLabel", activityLabel.getLabel());
                            jsonObject1.put("color", mcontext.getResources().getColor(R.color.activityColor));
                            jsonArray.put(jsonObject1);
                        }
                    } else {
                        ActivityLabel previousActivity = newActivities.get(i - 1);
                        int diff = startFrame - previousActivity.getEndFrame();
                        if (diff > 0) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("startFrame", Math.round(previousActivity.getEndFrame() * frameDuration) + 1);
                            jsonObject.put("endFrame", Math.round(startFrame * frameDuration) - 1);
                            jsonObject.put("activityLabel", "empty");
                            jsonObject.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                            jsonArray.put(jsonObject);
                        }

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("startFrame", Math.round(startFrame * frameDuration));
                        jsonObject.put("endFrame", Math.round(endFrame * frameDuration));
                        jsonObject.put("activityLabel", activityLabel.getLabel());
                        jsonObject.put("color", mcontext.getResources().getColor(R.color.activityColor));
                        jsonArray.put(jsonObject);
                    }

                    if (i == newActivities.size() - 1) {
                        JSONObject jsonObject = jsonArray.getJSONObject(jsonArray.length() - 1);
                        if (jsonObject.getInt("endFrame") < videoDuration) {
                            JSONObject jsonObject2 = new JSONObject();
                            jsonObject2.put("startFrame", jsonObject.getInt("endFrame") + 1);
                            jsonObject2.put("endFrame", videoDuration);
                            jsonObject2.put("activityLabel", "empty");
                            jsonObject2.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                            jsonArray.put(jsonObject2);
                        }
                    }
                }
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("startFrame", 0);
                jsonObject.put("endFrame", videoDuration);
                jsonObject.put("activityLabel", "empty");
                jsonObject.put("color", mcontext.getResources().getColor(R.color.noActivityColor));
                jsonArray.put(jsonObject);
            }
        } catch (JSONException ex) {
            Log.d("Activity segments", "Segments creation cause" + ex.getCause());
        }
        return jsonArray;
    }
}
