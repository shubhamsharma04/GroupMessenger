package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by opensam on 3/7/17.
 */

public class MsgObject  implements Comparable<MsgObject>{

    private int id;

    private String msg;

    private int pId;

    private int proposedId;

    private int agreedId;

    private boolean isDeliverable;

    private int action;

    public int getAction() {
        return action;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getpId() {
        return pId;
    }

    public void setpId(int pId) {
        this.pId = pId;
    }

    public int getProposedId() {
        return proposedId;
    }

    public void setProposedId(int proposedId) {
        this.proposedId = proposedId;
    }

    public int getAgreedId() {
        return agreedId;
    }

    public void setAgreedId(int agreedId) {
        this.agreedId = agreedId;
    }

    public boolean isDeliverable() {
        return isDeliverable;
    }

    public void setDeliverable(boolean isDeliverable) {
        this.isDeliverable = isDeliverable;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + agreedId;
        result = prime * result + (id ^ (id >>> 32));
        result = prime * result + (isDeliverable ? 1231 : 1237);
        result = prime * result + ((msg == null) ? 0 : msg.hashCode());
        result = prime * result + pId;
        result = prime * result + proposedId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MsgObject other = (MsgObject) obj;
        if (id != other.id)
            return false;
        if (msg == null) {
            if (other.msg != null)
                return false;
        } else if (!msg.equals(other.msg))
            return false;
        return true;
    }

    @Override
    public int compareTo(MsgObject o) {
        int result = 0;
        if (this.getAgreedId() == o.getAgreedId()) {
            result = this.getpId() - o.getpId();
        } else {
            result = this.getAgreedId() - o.getAgreedId();
        }
        return result;
    }

    @Override
    public String toString() {
        return "MsgObject{" +
                "action=" + action +
                ", id=" + id +
                ", msg='" + msg + '\'' +
                ", pId=" + pId +
                ", proposedId=" + proposedId +
                ", agreedId=" + agreedId +
                ", isDeliverable=" + isDeliverable +
                '}';
    }
}
