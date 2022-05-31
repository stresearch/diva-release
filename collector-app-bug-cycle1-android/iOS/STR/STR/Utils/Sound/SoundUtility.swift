//
//  SoundUtility.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import AVFoundation
import AudioToolbox
import Foundation

public enum SoundType: String {

    case industrial
    case bells

    var fileName: String {
        switch self {
        case .industrial: return "industrial_alarm"
        case .bells: return "bells_sound"
        }
    }

    var fileExt: String {
        switch self {
        case .industrial: return "caf"
        case .bells: return "caf"
        }
    }
}

class Sound {

    static var audioPlayer = AVAudioPlayer()

    /** Play one time any sound from bundle */
    class func playSystemSound(soundType: SoundType) {
        SystemSoundID.playFileNamed(fileName: soundType.fileName, withExtenstion: soundType.fileExt)
    }

    /// Play Audio with AVAudioPlayer
    class func playAudio(soundType: SoundType) {

        let fileUrl = Bundle.main.path(forResource: soundType.fileName, ofType: soundType.fileExt)

        if let url = fileUrl {
            do {
                audioPlayer = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: url))
                let session = AVAudioSession.sharedInstance()
                try session.setCategory(
                    AVAudioSession.Category.playback,
                    mode: AVAudioSession.Mode.spokenAudio,
                    options: AVAudioSession.CategoryOptions.duckOthers
                )
                audioPlayer.numberOfLoops = -1
                audioPlayer.play()

            } catch { print(error as Any) }
        }
    }

    class func stopAudio() {

        if audioPlayer.isPlaying { audioPlayer.stop() }
    }
}

extension SystemSoundID {

    static func playFileNamed(fileName: String, withExtenstion fileExtension: String) {
        var sound: SystemSoundID = 0
        if let soundURL = Bundle.main.url(forResource: fileName, withExtension: fileExtension) {
            AudioServicesCreateSystemSoundID(soundURL as CFURL, &sound)
            AudioServicesPlaySystemSound(sound)
        }
    }
}
