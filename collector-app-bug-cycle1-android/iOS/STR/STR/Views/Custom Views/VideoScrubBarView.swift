//
//  VideoScrubBarView.swift
//  STR
//
//  Created by Srujan on 07/01/20.
//  
//

import UIKit
import AVFoundation

protocol EditScrubBarDelegate: class {
    func didDragScrub(value: Float, ended: Bool)
}

@IBDesignable class VideoScrubBarView: UIView {
    
    @IBOutlet weak var totalDurationLbl: UILabel!
    @IBOutlet weak var currentDurationLbl: UILabel!
    @IBOutlet weak var slider: UISlider!
    @IBOutlet weak var sliderColoredView: UIView!
    @IBOutlet weak var sliderColoredViewLbl: UILabel!
    @IBOutlet weak var sliderView: UIView!
    
    private var bufferSlider: BufferSlider!
    
    fileprivate var isUpdateTime = false
    weak var playerController: PlayerController?
    
    weak var delegate: EditScrubBarDelegate?
    
    deinit { Log("\(self) I'm gone ") }
    
    class func instanceFromNib() -> VideoScrubBarView? {
        let view = UINib(nibName: "VideoScrubBarView", bundle: nil).instantiate(
            withOwner: nil,
            options: nil
        ).first as? VideoScrubBarView
        //view?.addSlider()
        return view
    }
    
    private func addSlider() {
        let slider = BufferSlider()
        self.sliderView.addSubview(slider)
        slider.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            slider.leadingAnchor.constraint(equalTo: self.sliderView.leadingAnchor),
            slider.trailingAnchor.constraint(equalTo: self.sliderView.trailingAnchor),
            slider.centerYAnchor.constraint(equalTo: sliderView.centerYAnchor),
            slider.heightAnchor.constraint(equalToConstant: 6),
        ])
        self.bufferSlider = slider
    }
    
    @IBAction func sliderChanged(_ sender: UISlider, forEvent event: UIEvent) {
        if let touchEvent = event.allTouches?.first {
            if touchEvent.phase != .ended {
                self.playerController?.pause()
                delaySeekTimeNew()
                delegate?.didDragScrub(value: Float(self.slider.value), ended: false) // UnComment
            }
            if touchEvent.phase == .ended {
                delaySeekTimeNew()
                delegate?.didDragScrub(value: Float(self.slider.value), ended: true)
            }
        }
    }
    
    @objc func delaySeekTime() {
        let time =  CMTimeMake(value: Int64(self.slider.value), timescale: 1)
        self.playerController?.seek(to: time, completionHandler: { [unowned self] (finish) in
            self.isUpdateTime = false
            if let controller = self.playerController, controller.playbackState == .stopped {
                self.playerController?.playFromCurrentTime()
                
            }
            else {
                self.playerController?.pause()
                
                self.timerObserver(time: .zero)
                //                self.playerController?.playFromCurrentTime()
                
                self.playerController?.playFromScrolledTime()
            }
        })
    }
    
    func delaySeekTimeNew() {
        let digit1 = self.slider.value.truncatingRemainder(dividingBy: 1)
        let digit2 = (digit1 * 10).truncatingRemainder(dividingBy: 1)
        
        let time =  CMTimeMake(value: Int64(self.slider.value), timescale: 1) + CMTimeMake(value: Int64(digit1 * 10), timescale: 10) + CMTimeMake(value: Int64(digit2 * 10), timescale: 100)
        
       let time1 =  CMTimeMake(value: Int64(1), timescale: 100)
        let time2 =  CMTimeMake(value: Int64(7), timescale: 100)
        
        self.playerController?.seekToTime(to: time, toleranceBefore: time1, toleranceAfter: time2)
    }
    
}

extension VideoScrubBarView {
    func timerObserver(time: TimeInterval) {
        if let duration = playerController?.asset?.duration ,
            !duration.isIndefinite ,
            !isUpdateTime {
            if self.slider.maximumValue != Float(duration.seconds) {
                self.slider.maximumValue = Float(duration.seconds)
            }
            self.currentDurationLbl.text = DateHelper.convertformat1(second: time)
            self.totalDurationLbl.text = DateHelper.convertformat1(second: duration.seconds)
            self.slider.setValue(Float(time), animated: true)
        }
    }
    
}
