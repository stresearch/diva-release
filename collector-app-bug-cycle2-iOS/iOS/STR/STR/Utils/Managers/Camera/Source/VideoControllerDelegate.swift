//
//  EasyCamViewControllerDelegate.swift
//  CIFaceMask
//
//  Created by Tushar on 8/13/18.
//  Copyright © 2018 BTC Soft. All rights reserved.
//

import AVFoundation

import UIKit

// MARK: Public Protocol Declaration

/// Delegate for CameraController

public protocol VideoControllerDelegate: class {
    
    /**
     CameraControllerDelegate function called when the takePhoto() function is called.
     
     - Parameter cameraController: Current CameraController session
     - Parameter photo: UIImage captured from the current session
     */
    
    func cameraController(_ cameraController: VideoController, didTake photo: UIImage)
    
    /**
     CameraControllerDelegate function called when CameraController begins recording video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter camera: Current camera orientation
     */
    
    func cameraController(_ cameraController: VideoController, didBeginRecordingVideo camera: VideoController.CameraSelection, zView: UIView)
    
    /**
     CameraControllerDelegate function called when CameraController finishes recording video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter camera: Current camera orientation
     */
    
    func cameraController(_ cameraController: VideoController, didFinishRecordingVideo camera: VideoController.CameraSelection)
    
    /**
     CameraControllerDelegate function called when CameraController is done processing video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter url: URL location of video in temporary directory
     */
    
    func cameraController(_ cameraController: VideoController, didFinishProcessVideoAt url: URL)
    
    
    /**
     CameraControllerDelegate function called when CameraController fails to record a video.
     
     - Parameter cameraController: Current CameraController session
     - Parameter error: An error object that describes the problem
     */
    func cameraController(_ cameraController: VideoController, didFailToRecordVideo error: Error)
    
    /**
     CameraControllerDelegate function called when CameraController switches between front or rear camera.
     
     - Parameter cameraController: Current CameraController session
     - Parameter camera: Current camera selection
     */
    
    
    func cameraController(_ cameraController: VideoController, didSwitchCameras camera: VideoController.CameraSelection)
    
    /**
     CameraControllerDelegate function called when CameraController view is tapped and begins focusing at that point.
     
     - Parameter cameraController: Current CameraController session
     - Parameter point: Location in view where camera focused
     
     */
    
    func cameraController(_ cameraController: VideoController, didSingleFocusAtPoint point: CGPoint)
    
  
    func cameraController(_ cameraController: VideoController, didDoubleFocusAtPoint point: CGPoint)
    
    /**
     CameraControllerDelegate function called when CameraController view changes zoom level.
     
     - Parameter cameraController: Current CameraController session
     - Parameter zoom: Current zoom level
     */
    
  func cameraController(_ cameraController: VideoController, didChangeZoomLevel zoom: CGFloat, zView: UIView, direction: String, gesture: UIPinchGestureRecognizer, location: CGPoint)
    
    /**
     CameraControllerDelegate function called when FaceMaskRecorder starts recording.
     
     - Parameter faceMaskRecorder: Current faceMaskRecorder session
     - Parameter imageSampleBuffer: Image Sample Buffer of current frame
     */
    
    /**
     CameraControllerDelegate function called when CameraController frame changes
     - Parameter cameraController: Current CameraController session
     - Parameter index: Current frame index
     */
    func cameraController(_ cameraController: VideoRecorder, didChangeFrame index: Int)
    
    func cameraController(_ faceMaskRecorder: VideoRecorder, didUpdate maskFrameRect: CGRect?,bufferFrameRect: CGRect?)
    
    func cameraController(_ faceMaskRecorder: VideoRecorder, didUpdate filteredImage: CIImage?)
    
    func cameraController_enableButtonView()
    
    func cameraController_disableButtonView()
    
    func cameraController(_ cameraController: VideoRecorder, didVideoOutputSampleBufferChange buffer: Bool)
}

public extension VideoControllerDelegate {
    
    func cameraController(_ faceMaskRecorder: VideoRecorder, didUpdate filteredImage: CIImage?) {
        
    }
    
    func cameraController(_ cameraController: VideoController, didTake photo: UIImage) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: VideoController, didBeginRecordingVideo camera: VideoController.CameraSelection) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: VideoController, didFinishRecordingVideo camera: VideoController.CameraSelection) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: VideoController, didFinishProcessVideoAt url: URL) {
        // Optional
    }
    
    func cameraController(_ cameraController: VideoController, didFailToRecordVideo error: Error) {
        // Optional
    }
    
    func cameraController(_ cameraController: VideoController, didSwitchCameras camera: VideoController.CameraSelection) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: VideoController, didFocusAtPoint point: CGPoint) {
        // Optional
    }
    
    
    func cameraController(_ cameraController: VideoController, didChangeZoomLevel zoom: CGFloat) {
        // Optional
    }
    
    func cameraController(_ faceMaskRecorder: VideoRecorder,didUpdate maskFrameRect: CGRect?,bufferFrameRect: CGRect?) {
        // Optional
    }
}



class ResizableView: UIView {

    enum Edge {
        case topLeft, topRight, bottomLeft, bottomRight, none
    }

    static var edgeSize: CGFloat = 44.0
    private typealias `Self` = ResizableView

    var currentEdge: Edge = .none
    var touchStart = CGPoint.zero
    var isDefaultObject = false
  
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        return nil
    }
  
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if let touch = touches.first {

            touchStart = touch.location(in: self)

            currentEdge = {
                if self.bounds.size.width - touchStart.x < Self.edgeSize && self.bounds.size.height - touchStart.y < Self.edgeSize {
                    return .bottomRight
                } else if touchStart.x < Self.edgeSize && touchStart.y < Self.edgeSize {
                    return .topLeft
                } else if self.bounds.size.width-touchStart.x < Self.edgeSize && touchStart.y < Self.edgeSize {
                    return .topRight
                } else if touchStart.x < Self.edgeSize && self.bounds.size.height - touchStart.y < Self.edgeSize {
                    return .bottomLeft
                }
                return .none
            }()
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        if let touch = touches.first {
            let currentPoint = touch.location(in: self)
            let previous = touch.previousLocation(in: self)

            let originX = self.frame.origin.x
            let originY = self.frame.origin.y
            let width = self.frame.size.width
            let height = self.frame.size.height

            let deltaWidth = currentPoint.x - previous.x
            let deltaHeight = currentPoint.y - previous.y

            switch currentEdge {
            case .topLeft:
                self.frame = CGRect(x: originX + deltaWidth, y: originY + deltaHeight, width: width - deltaWidth, height: height - deltaHeight)
            case .topRight:
                self.frame = CGRect(x: originX, y: originY + deltaHeight, width: width + deltaWidth, height: height - deltaHeight)
            case .bottomRight:
                self.frame = CGRect(x: originX, y: originY, width: currentPoint.x + deltaWidth, height: currentPoint.y + deltaWidth)
            case .bottomLeft:
                self.frame = CGRect(x: originX + deltaWidth, y: originY, width: width - deltaWidth, height: height + deltaHeight)
              
            default:
                // Moving
                self.center = CGPoint(x: self.center.x + currentPoint.x - touchStart.x,
                                      y: self.center.y + currentPoint.y - touchStart.y)
            }
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        currentEdge = .none
    }
}
