//
//  PlayerView.swift
//  STR
//
//  Created by Srujan on 07/01/20.
//  
//

import AVFoundation
import UIKit

class VideoPlayerView: UIView {
    
    deinit {
        Log("\(self) I'm gone ")
    }

    override static var layerClass: AnyClass {
        return AVPlayerLayer.self
    }

    var playerLayer: AVPlayerLayer {
        return layer as! AVPlayerLayer
    }
    
    var player: AVPlayer? {
        get {
            return playerLayer.player
        }
        set {
            playerLayer.player = newValue
        }
    }
}
