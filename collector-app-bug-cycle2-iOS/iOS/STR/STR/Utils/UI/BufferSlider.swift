//
//  BufferSlider.swift
//  slider
//
//  Created by Srujan on 22/01/20.
//  
//

import UIKit

public protocol BufferSliderDelegate: class {
    func SliderBeginDragging(slider: BufferSlider)
    func SliderEndDragging(slider: BufferSlider)
    func SliderScrub(slider: BufferSlider)
}

public enum ScrollState: Int {
    case beginDragging = 0
    case endDragging
    case scrub
    public var description: String {
        get {
            switch self {
            case .beginDragging:
                return "BeginDragging"
            case .endDragging:
                return "EndDragging"
            case .scrub:
                return "Scrub"
            }
        }
    }
}

public class BufferSlider: UIControl {
    
    public weak var sliderDelegate: BufferSliderDelegate?
    
    var currentPosition : Float = 0.0 {
        didSet {
             updateLayers()
        }
    }
    
    var currentBuffer : Float = 0.0 {
        didSet {
             updateLayers()
        }
    }
    
    var backgroundLayerColor : UIColor = UIColor.darkGray
    var progressLayerColor : UIColor = UIColor.orange
    var bufferLayerColor : UIColor = UIColor.darkGray
    var positionRingLayerColor : UIColor = UIColor.white
    
    private var backgroundLayer : CAShapeLayer!
    private var progressLayer : CAShapeLayer!
    private var bufferLayer : CAShapeLayer!
    private var positionRingLayer : CAShapeLayer!
    
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        initialize()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initialize()
    }
    
    override public func draw(_ rect: CGRect){
        updateLayers()
    }
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        initialize()
    }
    
    private func initialize() {
        self.layer.sublayers?.forEach({ (layer) in
            layer.removeFromSuperlayer()
        })
        let frameHeight = self.frame.size.height
        let frameWidth = self.frame.size.width
        
        self.backgroundColor = UIColor.clear
        
        backgroundLayer = CAShapeLayer()
        backgroundLayer.frame = CGRect(x: 0, y: 0, width: frameWidth, height: frameHeight)
        backgroundLayer.path = UIBezierPath(rect: CGRect(x: 0, y: (frameHeight / 2) - frameHeight / 4, width: frameWidth, height: frameHeight / 2.0)).cgPath
        backgroundLayer.fillColor = backgroundLayerColor.withAlphaComponent(0.5).cgColor
        backgroundLayer.backgroundColor = UIColor.clear.cgColor
        
        progressLayer = CAShapeLayer()
        progressLayer.frame = CGRect(x: 0, y: 0, width: frameWidth, height: frameHeight)
        
        bufferLayer = CAShapeLayer()
        bufferLayer.frame = CGRect(x: 0, y: 0, width: frameWidth, height: frameHeight)
        
        positionRingLayer = CAShapeLayer()
        positionRingLayer.frame = CGRect(x: 0, y: 0, width: frameWidth, height: frameHeight)
        
        self.layer.addSublayer(backgroundLayer)
        self.layer.addSublayer(bufferLayer)
        self.layer.addSublayer(progressLayer)
        self.layer.addSublayer(positionRingLayer)
        updateLayers()
    }
    
    private func updateLayers() {
        updateProgressLine()
        updateBufferLine()
        updatePositionRing()
    }
    
    private func updateProgressLine() {
        var w = (self.frame.size.width * CGFloat(currentPosition)) + self.frame.size.height / 4
        
        if w > self.frame.size.width {
            w = self.frame.size.width
        }
        
        progressLayer.path = UIBezierPath(rect: CGRect(x: 0, y: (self.frame.size.height / 2) - self.frame.size.height / 4, width: w, height: self.frame.size.height / 2)).cgPath
        progressLayer.fillColor = progressLayerColor.cgColor
        progressLayer.backgroundColor = UIColor.clear.cgColor
    }
    
    private func updateBufferLine() {
        let w = self.frame.size.width * CGFloat(currentBuffer)
        
        bufferLayer.path = UIBezierPath(rect: CGRect(x: 0, y: (self.frame.size.height / 2) - self.frame.size.height / 4, width: w, height: self.frame.size.height / 2)).cgPath
        bufferLayer.fillColor = bufferLayerColor.cgColor
        bufferLayer.backgroundColor = UIColor.clear.cgColor
    }
    
    private func updatePositionRing() {
        var x = self.frame.size.width * CGFloat(currentPosition)
        
        if x > self.frame.size.width - self.frame.size.height {
            x = self.frame.size.width - self.frame.size.height
        }
        
        let progressHeight = self.frame.size.height
        let diameter = progressHeight * 4 // 2x of progress height
        let newX = x - (diameter/2)
        let newY = -(diameter/2) + progressHeight/2
        positionRingLayer.path = UIBezierPath(ovalIn: CGRect(x: newX, y: newY, width: diameter, height: diameter)).cgPath
        positionRingLayer.fillColor = positionRingLayerColor.cgColor
        positionRingLayer.backgroundColor = UIColor.clear.cgColor
    }
    
    override public func beginTracking(_ touch: UITouch, with event: UIEvent?) -> Bool {
        super.beginTracking(touch, with: event)
        print("beginTrackingWithTouch")
        sliderDelegate?.SliderBeginDragging(slider: self)
        return true
    }
    
    
    override public func continueTracking(_ touch: UITouch, with event: UIEvent?) -> Bool {
        super.continueTracking(touch, with: event)
        
        sliderDelegate?.SliderScrub(slider: self)
        let point = touch.location(in: self)
        
        _ = (self.frame.size.width * CGFloat(currentBuffer)) - self.frame.size.height
        if  (point.x > 0) // if (point.x < _xb) && (point.x > 0) used for seek and play
        {
            currentPosition = Float(point.x / self.frame.size.width)
            self.setNeedsDisplay()
        }
        return true
    }
    override public func endTracking(_ touch: UITouch?, with event: UIEvent?){
        super.endTracking(touch, with: event)
        sliderDelegate?.SliderEndDragging(slider: self)
    }
    
    override public func cancelTracking(with event: UIEvent?) {
        super.cancelTracking(with: event)
        print("cancelTrackingWithEvent")
        sliderDelegate?.SliderEndDragging(slider: self)
    }
}
