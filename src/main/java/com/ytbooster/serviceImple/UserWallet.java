package com.ytbooster.serviceImple;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ytbooster.model.User;
import com.ytbooster.repository.UserRepository;
import com.ytbooster.service.UserWalletServices;

@Service
public class UserWallet implements UserWalletServices {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public boolean walletDeduct(BigDecimal amount, String email) {
        User user = userRepository.findByEmailForUpdate(email); // PESSIMISTIC WRITE
        if (user == null) return false;

        BigDecimal wallet = user.getWallet() != null ? user.getWallet() : BigDecimal.ZERO;
        if (wallet.compareTo(amount) < 0) return false;

        user.setWallet(wallet.subtract(amount));
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean walletRecharge(BigDecimal amount, String email) {
        User user = userRepository.findByEmailForUpdate(email); // PESSIMISTIC WRITE
        if (user == null) return false;

        BigDecimal wallet = user.getWallet() != null ? user.getWallet() : BigDecimal.ZERO;
        user.setWallet(wallet.add(amount));
        userRepository.save(user);
        return true;
    }
}
