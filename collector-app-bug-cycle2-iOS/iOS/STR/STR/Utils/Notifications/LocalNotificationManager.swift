//
//  LocalNotificationManager.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import Foundation
import UIKit
import UserNotifications

class LocalNotificationManager: NSObject {

    func createNotificationAfter(minutes: Double, minutesStr: String) {

        let content = UNMutableNotificationContent()
        var timerInterval = (minutes * 60) - 3  // 5 seconds early  assuming api delay

        if timerInterval < 0 { timerInterval = 1 }

        // let minutesStr = String(Int(minutes))
        let body = "ok"
        content.body = body
        content.title = "hello"
        content.sound = UNNotificationSound(
            named: UNNotificationSoundName(rawValue: "Sound file name")
        )
        content.badge = NSNumber(value: UIApplication.shared.applicationIconBadgeNumber + 1)

        content.userInfo = [:]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: timerInterval, repeats: false)

        let request = UNNotificationRequest(
            identifier: "some id",
            content: content,
            trigger: trigger
        )

        // Schedule the notification.
        let center = UNUserNotificationCenter.current()
        center.add(request)

    }

 

    func deleteNotification(id: String) {

        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [id])

    }

    static func deleteAllLocalNotificationsRequests() {
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()
    }

    static func removeAllDeliveredNotifications() {
        let center = UNUserNotificationCenter.current()
        center.removeAllDeliveredNotifications()
    }

}
