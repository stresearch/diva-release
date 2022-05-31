//
//  SynchronizationManager.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import Foundation
import Reachability

enum SynchronizationStatus: String {
    case Synced
    case NeedsCreate
    case NeedsUpdate
    case NeedsDelete
    case Syncing

}

class SynchronizationManager {

    // MARK: - Singleton
    static let sharedInstance = SynchronizationManager()

    private init() {}

    // MARK: - Properties
    private var isReachable = false

    let reachability = try? Reachability()

    // MARK: - Utils
    func start() {

        guard isReachable else { return }

    }

    func addObserver() {

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(reachabilityChanged(note:)),
            name: .reachabilityChanged,
            object: reachability
        )
        do { try SynchronizationManager.sharedInstance.reachability?.startNotifier() } catch {
            print("could not start reachability notifier")
        }

    }

    @objc func reachabilityChanged(note: Notification) {

        let reachability = note.object as! Reachability

        switch reachability.connection {
        case .wifi, .cellular: self.isReachable = true

        case .unavailable, .none: self.isReachable = false
        }
        start()

    }

}
