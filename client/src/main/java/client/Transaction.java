package client;

import java.io.Serializable;

/*
 * Class of Transaction
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */
public class Transaction implements Serializable {
    private String payer;
    private String receiver;
    private float amount;

    /**
     * Creates a Instance of Transaction
     * 
     * @param payer    The user who is paying
     * @param receiver The user who is receiving
     * @param amount   Amount of Transaction
     */
    public Transaction(String payer, String receiver, float amount) {
        this.payer = payer;
        this.receiver = receiver;
        this.amount = amount;
    }

    /**
     * Gets the username of payer
     * 
     * @return The user who is paying
     */
    public String getPayer() {
        return this.payer;
    }

    /**
     * Gets the username of receiver
     * 
     * @return The user who is receiving
     */
    public String getReceiver() {
        return this.receiver;
    }

    /**
     * Gets the amount of transaction
     * 
     * @return The amount of transaction
     */
    public Float getAmount() {
        return this.amount;
    }

    /**
     * Visual representation of the Transaction
     */
    public String toString() {
        return this.payer + ":" + this.receiver + ":" + this.amount;
    }
}
