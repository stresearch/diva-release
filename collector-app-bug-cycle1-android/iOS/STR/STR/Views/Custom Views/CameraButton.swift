//
//  CameraButton.swift
//  STR
//
//  Created by Srujan on 07/01/20.
//  
//

import UIKit

//MARK: Public Protocol Declaration

/// Delegate for SwiftyCamButton

public protocol CameraButtonDelegate: class {
    /// Called when UITapGestureRecognizer begins
    
    func buttonWasTapped()
  
    /// Called when time interval updated
  
    func updatedTimerValue(time: Double)
}

// MARK: Public View Declaration


/// UIButton Subclass for Capturing Photo and Video with SwiftyCamViewController

open class CameraButton: UIButton {
    
    /// Delegate variable
    
    public weak var delegate: CameraButtonDelegate?
    
    // Sets whether button is enabled
    
    public var buttonEnabled = true
    
    /// Initialization Declaration
    
    fileprivate var isTapped = false
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        //createGestureRecognizers()
        self.addTarget(delegate, action: #selector(self.tap), for: .touchUpInside)
    }
    
    /// Initialization Declaration
    
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        //createGestureRecognizers()
        self.addTarget(delegate, action: #selector(self.tap), for: .touchUpInside)
    }
    
    /// UITapGestureRecognizer Function
    
    @objc fileprivate func tap() {
        guard buttonEnabled == true else {
            return
        }
        isTapped = !isTapped
        if isTapped {
            
        } else {
            invalidate()
        }
        delegate?.buttonWasTapped()
    }
    
    /// Timer Finished
    
    @objc fileprivate func timerFinished() {
        invalidate()
    }
    
    // End timer if UILongPressGestureRecognizer is ended before time has ended
    
    fileprivate func invalidate() {
        isTapped = false
    }
    
    // Add Tap gesture recognizers
    
    fileprivate func createGestureRecognizers() {
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(CameraButton.tap))
        self.addGestureRecognizer(tapGesture)
    }
}

extension CameraController : CameraButtonDelegate {
  
    /// Called when time interval updated
  
    public func updatedTimerValue(time: Double) {
      
    }
    
    /// Set UITapGesture to start/stop video
    
    public func buttonWasTapped() {
        if isVideoRecording {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }
  
}

