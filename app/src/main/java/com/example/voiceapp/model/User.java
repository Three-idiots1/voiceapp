package com.example.voiceapp.model;

public class User {
    private Long id; // ID 필드 추가
    private String name;
    private int age;
    private String gender;
    private int height_cm;
    private double weight;

    // 몸무게를 포함한 생성자
    public User(String name, int age, String gender, int height_cm, double weight) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.height_cm = height_cm;
        this.weight = weight;
    }

    // 기본 생성자 (Retrofit과 같은 라이브러리에서 필요할 수 있음)
    public User() {
    }

    // Getter methods
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }
    public int getTall() {
        return height_cm;
    }

    // 키에 접근하는 추가 메서드 (getHeightCm 대체)
    public int getHeightCm() {
        return height_cm;
    }

    // 몸무게 getter 추가
    public double getWeight() {
        return weight;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setTall(int tall) {
        this.height_cm = tall;
    }

    public void setHeightCm(int height_cm) {
        this.height_cm = height_cm;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    // toString 메소드 (디버깅용)
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", height=" + height_cm +
                ", weight=" + weight +
                '}';
    }
}