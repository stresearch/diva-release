//
//  PreviewVideoViewController.swift
//  CIFaceMask
//
//  Created by Srujan on 04/12/19.
//  Copyright © 2019 BTC Soft. All rights reserved.
//

import UIKit
import AVFoundation

class VideoPreviewVC: UIViewController {

  @IBOutlet weak var playButton: UIButton!
  @IBOutlet weak var backButton: UIButton!
  @IBOutlet weak var viewPreview: UIView!
  @IBOutlet weak var labelTimer :UILabel!
  
  var player: AVPlayer! = nil
  var playerLayer: AVPlayerLayer! = nil
  var videoUrl: URL?
  var subjectCordinates: [Cordinate]?
  var subjectActivities: [Activity]?
  var timer: Timer?
  var minTimerValue = 0
  var maxTimerValue: Int?
  var subjectView = ResizableView()
  var activityLabel = UILabel()
  var playmode = PlayMode.play
  
  enum PlayMode {
      case play,pause
  }
  
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }
    
    override func viewDidAppear(_ animated: Bool) {
      addVideoToPreviewLayer()
      labelTimer.text = "  Time: \(minTimerValue)   "
    }

    override func viewDidDisappear(_ animated: Bool) {
      timer?.invalidate()
      playerLayer?.removeFromSuperlayer()
      activityLabel.removeFromSuperview()
    }
    
    private func addVideoToPreviewLayer() {
      if let url = videoUrl {
        playerLayer?.removeFromSuperlayer()
        player = AVPlayer(url: url)
        playerLayer = AVPlayerLayer(player: player)
        playerLayer.frame = self.view.bounds
          
        viewPreview.layer.addSublayer(playerLayer)
        
        subjectView = ResizableView(frame: (subjectCordinates?[0].frame)!)
        self.subjectView.layer.borderWidth = 2.0
        self.subjectView.layer.borderColor = #colorLiteral(red: 0.9250000119, green: 0.6669999957, blue: 0, alpha: 1)
        view.addSubview(subjectView)
        
        activityLabel.textColor = UIColor.black
        activityLabel.backgroundColor = #colorLiteral(red: 0.9250000119, green: 0.6669999957, blue: 0, alpha: 1)
        activityLabel.numberOfLines = 1
        activityLabel.text = ""
        activityLabel.textAlignment = .center
        activityLabel.layer.borderColor = UIColor.black.cgColor
        activityLabel.layer.borderWidth = 1.0
        activityLabel.frame = CGRect(x: 50, y: 50, width: 0, height: activityLabel.frame.height)
        activityLabel.sizeToFit()
        self.activityLabel.center.x = self.view.center.x
        view.addSubview(activityLabel)
      }
    }
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

  private func startTimer() {
    
    timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true, block: { (timer) in
      //self.labelTimer.text = "  Time: \(self.minTimerValue)   "
      self.minTimerValue += 1
      if let index = self.subjectCordinates?.firstIndex(where: { $0.time == self.minTimerValue }) {
        UIView.animate(withDuration: 0.5) {
          self.subjectView.frame = (self.subjectCordinates?[index].frame)!
        }
      }
      
      if let activityIndex = self.subjectActivities?.firstIndex(where: {self.minTimerValue >= $0.startTime && self.minTimerValue <= $0.endTime}) {
        self.activityLabel.backgroundColor = UIColor.yellow
        self.activityLabel.text = "\(self.subjectActivities?[activityIndex].name ?? "")"
        self.activityLabel.sizeToFit()
        self.activityLabel.frame.origin.y = self.subjectView.frame.origin.y - self.activityLabel.frame.size.height
        self.activityLabel.frame.origin.x = self.subjectView.frame.origin.x
      } else {
        self.activityLabel.backgroundColor = UIColor.clear
        self.activityLabel.text = ""
      }
      
      if self.minTimerValue == self.maxTimerValue {
        timer.invalidate()
        self.subjectView.removeFromSuperview()
        self.playerLayer.removeFromSuperlayer()
        self.playmode = .play
        self.backButton.isHidden = false
        self.addVideoToPreviewLayer()
        self.playButton.setTitle("Play", for: .normal)
        self.minTimerValue = 0
      }
    })
  }
  
    private func updateSubjectCoordinates() {
        if let index = self.subjectCordinates?.firstIndex(where: { $0.time == self.minTimerValue }) {
          subjectCordinates?[index].frame = subjectView.frame
        } else {
          let newCoordinate = Cordinate(zframe: subjectView.frame, ztime: minTimerValue)
          subjectCordinates?.append(newCoordinate)
        }
    }
  
  //MARK:- Button Actions
  
  @IBAction func closeButtonAction(_ sender: UIButton) {
    
  }
  
  @IBAction func retakeButtonAction(_ sender: UIButton) {
    
    self.navigationController?.popViewController(animated: true)
  }
  
  @IBAction func playButtonAction(_ sender: UIButton) {
    
    self.backButton.isHidden = true
    
    switch playmode {
      
    case .play:
      updateSubjectCoordinates()
      subjectView.isUserInteractionEnabled = false
      playButton.setTitle("Pause", for: .normal)
      playmode = .pause
      player.play()
      startTimer()
      
    case .pause:
      subjectView.isUserInteractionEnabled = true
      playButton.setTitle("Play", for: .normal)
      playmode = .play
      player.pause()
      timer?.invalidate()
    }
  }
}
