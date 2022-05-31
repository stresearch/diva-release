//
//  EasyCamViewControllerDelegate.swift
//  STR
//
//  Created by Srujan on 07/01/20.
//  
//

import AVFoundation

import UIKit

// MARK: Public Protocol Declaration

/// Delegate for CameraController

public protocol CameraControllerDelegate: class {
    
    /**
     CameraControllerDelegate function called when the takePhoto() function is called.
     
     - Parameter cameraController: Current CameraController session
     - Parameter photo: UIImage captured from the current session
     */
    
    func cameraController(_ cameraController: CameraController, didTake photo: UIImage)
    
    /**
     CameraControllerDelegate function called when CameraController begins recording video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter camera: Current camera orientation
     */
    
    func cameraController(_ cameraController: CameraController, didBeginRecordingVideo camera: CameraController.CameraSelection)
    
    /**
     CameraControllerDelegate function called when CameraController finishes recording video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter camera: Current camera orientation
     */
    
    func cameraController(_ cameraController: CameraController, didFinishRecordingVideo camera: CameraController.CameraSelection)
    
    /**
     CameraControllerDelegate function called when CameraController is done processing video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter url: URL location of video in temporary directory
     */
    
    func cameraController(_ cameraController: CameraController, didFinishProcessVideoAt url: URL)
    
    
    /**
     CameraControllerDelegate function called when CameraController fails to record a video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter error: An error object that describes the problem
     */
    func cameraController(_ cameraController: CameraController, didFailToRecordVideo error: Error)
    
    /**
     CameraControllerDelegate function called when CameraController switches between front or rear camera.
     
     - Parameter cameraController: Current CameraController session
     - Parameter camera: Current camera selection
     */
    
    
    func cameraController(_ cameraController: CameraController, didSwitchCameras camera: CameraController.CameraSelection)
    
    /**
     CameraControllerDelegate function called when CameraController view is tapped and begins focusing at that point.
     
     - Parameter cameraController: Current CameraController session
     - Parameter point: Location in view where camera focused
     
     */
    
    func cameraController(_ cameraController: CameraController, didFocusAtPoint point: CGPoint)
    
    /**
     CameraControllerDelegate function called when CameraController view changes zoom level.
     
     - Parameter cameraController: Current CameraController session
     - Parameter zoom: Current zoom level
     */
    
    func cameraController(_ cameraController: CameraController, didChangeZoomLevel zoom: CGFloat)
    
    /**
     CameraControllerDelegate function called when FaceMaskRecorder starts recording.
     
     - Parameter faceMaskRecorder: Current faceMaskRecorder session
     - Parameter imageSampleBuffer: Image Sample Buffer of current frame
     */
    
    func cameraController(_ faceMaskRecorder: VideoRecorder,didUpdate maskFrameRect: CGRect?,bufferFrameRect: CGRect?)
    
    
}

public extension CameraControllerDelegate {
    
    func cameraController(_ cameraController: CameraController, didTake photo: UIImage) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: CameraController, didBeginRecordingVideo camera: CameraController.CameraSelection) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: CameraController, didFinishRecordingVideo camera: CameraController.CameraSelection) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: CameraController, didFinishProcessVideoAt url: URL) {
        // Optional
    }
    
    func cameraController(_ cameraController: CameraController, didFailToRecordVideo error: Error) {
        // Optional
    }
    
    func cameraController(_ cameraController: CameraController, didSwitchCameras camera: CameraController.CameraSelection) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: CameraController, didFocusAtPoint point: CGPoint) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: CameraController, didChangeZoomLevel zoom: CGFloat) {
        // Optional
    }
    
    func cameraController(_ faceMaskRecorder: VideoRecorder,didUpdate maskFrameRect: CGRect?,bufferFrameRect: CGRect?) {
        // Optional
    }
}



